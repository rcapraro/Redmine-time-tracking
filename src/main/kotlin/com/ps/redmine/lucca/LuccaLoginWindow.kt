package com.ps.redmine.lucca

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.jetbrains.cef.JCefAppConfig
import com.ps.redmine.resources.Strings
import com.ps.redmine.util.OSUtils
import com.ps.redmine.util.OperatingSystem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.cef.CefApp
import org.cef.CefClient
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefRendering
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefCookie
import org.cef.network.CefCookieManager
import java.awt.BorderLayout
import java.awt.Dialog
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.net.URI
import javax.swing.JFrame
import javax.swing.WindowConstants
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

internal object JcefRuntime {
    private var app: CefApp? = null
    private val openWindows = mutableSetOf<AutoCloseable>()

    @Synchronized
    fun app(): CefApp = app ?: start().also { app = it }

    @Synchronized
    fun register(window: AutoCloseable) {
        openWindows += window
    }

    @Synchronized
    fun unregister(window: AutoCloseable) {
        openWindows -= window
    }

    // CEF cannot be restarted once disposed within the same JVM, so this is only for application exit.
    // Browsers must be closed before the app they belong to is disposed.
    @Synchronized
    fun shutdown() {
        openWindows.toList().forEach { it.close() }
        app?.dispose()
        app = null
    }

    // Deletion is asynchronous; a flush queued behind it only completes once it has been applied, so a sign-in
    // started right after cannot pick the previous session up.
    suspend fun clearCookies() {
        val manager = synchronized(this) { app?.let { CefCookieManager.getGlobalManager() } } ?: return
        manager.deleteCookies("", "")
        val flushed = CompletableDeferred<Unit>()
        if (manager.flushStore { flushed.complete(Unit) }) withTimeoutOrNull(COOKIE_FLUSH_TIMEOUT) { flushed.await() }
    }

    private val COOKIE_FLUSH_TIMEOUT = 2.seconds

    private fun start(): CefApp {
        val config = JCefAppConfig.getInstance()
        config.cefSettings.windowless_rendering_enabled = false
        config.cefSettings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_DISABLE
        // No cache_path: the browser profile stays in memory; the session outlives it in the OS keychain.
        if (OSUtils.getCurrentOS() == OperatingSystem.MACOS) macFrameworksDir()?.let { pointCefAt(it, config.appArgsAsList) }
        CefApp.startup(config.appArgs)
        return CefApp.getInstance(config.appArgs, config.cefSettings)
    }

    // JCefAppConfig assumes the bundle layout (java.home/../Frameworks), which holds in a packaged app.
    // Some JDK managers (mise) flatten Contents/Home into the install root and keep Contents/ inside it.
    private fun macFrameworksDir(): File? {
        val javaHome = File(System.getProperty("java.home"))
        return listOf(File(javaHome, "../Frameworks"), File(javaHome, "Contents/Frameworks"))
            .map { it.normalize() }
            .firstOrNull { File(it, "Chromium Embedded Framework.framework").isDirectory }
    }

    private fun pointCefAt(frameworks: File, args: MutableList<String>) {
        val overridden = listOf("--framework-dir-path", "--browser-subprocess-path", "--main-bundle-path")
        args.removeAll { arg -> overridden.any { arg.startsWith(it) } }
        args += "--framework-dir-path=${File(frameworks, "Chromium Embedded Framework.framework")}"
        args += "--browser-subprocess-path=${File(frameworks, "jcef Helper.app/Contents/MacOS/jcef Helper")}"
        args += "--main-bundle-path=${File(frameworks, "jcef Helper.app")}"
    }
}

private class LuccaBrowserWindow private constructor(
    private val client: CefClient,
    private val luccaClient: LuccaClient,
    private val scope: CoroutineScope,
    title: String,
    private val onLoggedIn: (LuccaSession) -> Unit,
    private val onClosedByUser: () -> Unit,
) : AutoCloseable {
    private val browser: CefBrowser = client.createBrowser(luccaClient.baseUrl, CefRendering.DEFAULT, false)
    private val frame = JFrame(title)
    private val luccaHost = URI(luccaClient.baseUrl).host
    private var sessionCheck: Job? = null
    private var loggedIn = false
    private var closed = false

    init {
        client.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (frame.isMain) {
                    val url = browser.url
                    scope.launch { onPageLoaded(url) }
                }
            }
        })
        client.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            // Identity providers sometimes open a popup; keep the whole flow in this window.
            override fun onBeforePopup(
                browser: CefBrowser,
                frame: CefFrame,
                targetUrl: String?,
                targetFrameName: String?,
            ): Boolean {
                targetUrl?.let(browser::loadURL)
                return true
            }
        })
        frame.apply {
            // Stays usable while a modal dialog of the app is open.
            modalExclusionType = Dialog.ModalExclusionType.APPLICATION_EXCLUDE
            defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) {
                    close()
                    onClosedByUser()
                }
            })
            contentPane.add(browser.uiComponent, BorderLayout.CENTER)
            setSize(960, 760)
            setLocationRelativeTo(null)
            isVisible = true
        }
        JcefRuntime.register(this)
    }

    override fun close() {
        if (closed) return
        closed = true
        JcefRuntime.unregister(this)
        sessionCheck?.cancel()
        browser.close(true)
        client.dispose()
        frame.dispose()
    }

    private fun onPageLoaded(url: String?) {
        if (closed || loggedIn || url == null || !isSignedInPage(url)) return
        sessionCheck?.cancel()
        // The landing page may load before CEF has stored the session cookie, and Lucca may fail transiently:
        // a single-page app does not reload, so nothing else would trigger a new check.
        sessionCheck = scope.launch {
            repeat(SESSION_CHECK_ATTEMPTS) { attempt ->
                if (attempt > 0) delay(SESSION_CHECK_RETRY_DELAY)
                val session = acceptedSession()
                if (session != null) {
                    loggedIn = true
                    onLoggedIn(session)
                    return@launch
                }
            }
        }
    }

    private suspend fun acceptedSession(): LuccaSession? {
        val cookies = luccaCookies()
        // Prefer the HttpOnly cookies alone: they carry the session and keep the keychain entry small.
        val candidates = listOf(cookies.filter { it.httponly }, cookies)
            .map { set -> set.map { LuccaCookie(it.name, it.value) } }
            .filter { it.isNotEmpty() }
            .distinct()
        for (candidate in candidates) {
            val userId = userIdAcceptedByLucca(candidate) ?: continue
            return LuccaSession(userId, candidate)
        }
        return null
    }

    private fun isSignedInPage(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val path = uri.path.orEmpty()
        return uri.host == luccaHost && LOGIN_PATH_PREFIXES.none { path.startsWith(it) }
    }

    private suspend fun luccaCookies(): List<CefCookie> {
        val result = CompletableDeferred<List<CefCookie>>()
        val collected = mutableListOf<CefCookie>()
        val visiting = CefCookieManager.getGlobalManager().visitUrlCookies(luccaClient.baseUrl, true) { cookie, count, total, _ ->
            collected += cookie
            if (count == total - 1) result.complete(collected.toList())
            true
        }
        if (!visiting) return emptyList()
        // CEF never calls the visitor when there is no cookie at all.
        return withTimeoutOrNull(COOKIE_VISIT_TIMEOUT) { result.await() } ?: emptyList()
    }

    private suspend fun userIdAcceptedByLucca(cookies: List<LuccaCookie>): Int? = try {
        luccaClient.currentUserId(cookies)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    companion object {
        private val LOGIN_PATH_PREFIXES = listOf("/identity", "/login")
        private val COOKIE_VISIT_TIMEOUT = 2.seconds
        private const val SESSION_CHECK_ATTEMPTS = 5
        private val SESSION_CHECK_RETRY_DELAY = 1.seconds

        fun open(
            luccaClient: LuccaClient,
            scope: CoroutineScope,
            title: String,
            onLoggedIn: (LuccaSession) -> Unit,
            onClosedByUser: () -> Unit,
        ): LuccaBrowserWindow {
            val client = JcefRuntime.app().createClient()
            return try {
                LuccaBrowserWindow(client, luccaClient, scope, title, onLoggedIn, onClosedByUser)
            } catch (e: Throwable) {
                client.dispose()
                throw e
            }
        }
    }
}

@Composable
fun LuccaLoginWindow(
    luccaClient: LuccaClient,
    onLoggedIn: (LuccaSession) -> Unit,
    onDismiss: () -> Unit,
    onBrowserUnavailable: (Throwable) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val latestOnLoggedIn by rememberUpdatedState(onLoggedIn)
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val latestOnBrowserUnavailable by rememberUpdatedState(onBrowserUnavailable)
    LaunchedEffect(luccaClient) {
        val window = try {
            // Loading the Chromium libraries can be slow on a cold or slow disk; keep it off the UI thread.
            withContext(Dispatchers.IO) { JcefRuntime.app() }
            LuccaBrowserWindow.open(
                luccaClient = luccaClient,
                scope = scope,
                title = Strings["lucca_login_window_title"],
                onLoggedIn = { latestOnLoggedIn(it) },
                onClosedByUser = { latestOnDismiss() },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: LinkageError) {
            latestOnBrowserUnavailable(e)
            return@LaunchedEffect
        } catch (e: Exception) {
            latestOnBrowserUnavailable(e)
            return@LaunchedEffect
        }
        try {
            awaitCancellation()
        } finally {
            window.close()
        }
    }
}
