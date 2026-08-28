package com.ps.redmine.update

import com.ps.redmine.Version
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Service responsible for checking for application updates from GitHub releases.
 */
class UpdateService {
    private val httpClient = HttpClient(CIO) {
        followRedirects = true
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 10_000
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * Checks if a new version is available on GitHub releases.
     *
     * Returns null only when the check succeeded and no newer version exists. A failed check
     * throws, so the caller can tell "up to date" from "could not tell" and keep any update it
     * already knows about.
     *
     * @return UpdateInfo if an update is available, null if already up to date
     */
    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        val response: HttpResponse =
            httpClient.get("https://api.github.com/repos/rcapraro/Redmine-time-tracking/releases/latest") {
                header(HttpHeaders.Accept, "application/vnd.github.v3+json")
                header(HttpHeaders.UserAgent, "RedmineTime/${Version.VERSION}")
            }

        // expectSuccess is off on this client, so a 403 rate-limit would otherwise read as "up to date"
        if (response.status != HttpStatusCode.OK) {
            error("GitHub releases API returned ${response.status}")
        }

        val release = json.decodeFromString<GitHubRelease>(response.bodyAsText())
        val latestVersion = release.tagName.removePrefix("v")

        if (!isNewerVersion(Version.VERSION, latestVersion)) {
            return@withContext null
        }

        val platformAsset = getPlatformAsset(release.assets)
        UpdateInfo(
            version = latestVersion,
            downloadUrl = platformAsset?.browserDownloadUrl,
            releasePageUrl = release.htmlUrl,
            releaseNotes = release.body ?: "",
            publishedAt = release.publishedAt,
            fileSize = platformAsset?.size ?: -1L
        )
    }


    /**
     * Compares two version strings to determine if the second is newer.
     * Supports semantic versioning with optional pre-release suffix (e.g. "1.2.3", "2.0.0-beta").
     * Per semver, a version with a pre-release suffix is older than the same core without one,
     * so a user on "2.0.0-beta" will be offered "2.0.0" stable.
     */
    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentCore = current.substringBefore('-')
        val currentPre = current.substringAfter('-', missingDelimiterValue = "")
        val latestCore = latest.substringBefore('-')
        val latestPre = latest.substringAfter('-', missingDelimiterValue = "")

        val currentParts = currentCore.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latestCore.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLength) {
            val currentPart = currentParts.getOrNull(i) ?: 0
            val latestPart = latestParts.getOrNull(i) ?: 0
            when {
                latestPart > currentPart -> return true
                latestPart < currentPart -> return false
            }
        }

        return when {
            currentPre.isNotEmpty() && latestPre.isEmpty() -> true
            currentPre.isEmpty() && latestPre.isNotEmpty() -> false
            currentPre.isEmpty() && latestPre.isEmpty() -> false
            else -> latestPre > currentPre
        }
    }

    /**
     * Gets the appropriate asset for the current platform.
     */
    private fun getPlatformAsset(assets: List<GitHubAsset>): GitHubAsset? {
        val osName = System.getProperty("os.name").lowercase()

        return when {
            osName.contains("mac") -> {
                assets.find { it.name.endsWith(".dmg") }
            }

            osName.contains("win") -> {
                assets.find { it.name.endsWith(".msi") || it.name.endsWith(".exe") }
            }

            osName.contains("linux") -> {
                assets.find { it.name.endsWith(".deb") }
            }

            else -> null
        }
    }

    fun close() {
        httpClient.close()
    }
}

/**
 * Information about an available update.
 */
data class UpdateInfo(
    val version: String,
    val downloadUrl: String?,
    val releasePageUrl: String?,
    val releaseNotes: String,
    val publishedAt: String,
    val fileSize: Long = -1L
)

/**
 * GitHub release API response model.
 */
@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    val name: String,
    val body: String?,
    @SerialName("published_at")
    val publishedAt: String,
    @SerialName("html_url")
    val htmlUrl: String,
    val assets: List<GitHubAsset>
)

/**
 * GitHub release asset model.
 */
@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    val size: Long
)