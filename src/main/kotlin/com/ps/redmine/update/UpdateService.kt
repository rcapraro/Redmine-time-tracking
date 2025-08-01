package com.ps.redmine.update

import com.ps.redmine.Version
import io.ktor.client.*
import io.ktor.client.call.*
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
        engine {
            requestTimeout = 60_000
            // Enable HTTP/2 and connection pooling for better performance
            pipelining = true
            // Use custom dispatcher for better thread management
            dispatcher = kotlinx.coroutines.Dispatchers.IO
        }
        // Enable following redirects automatically
        followRedirects = true
        // Set reasonable timeouts for the entire request
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
     * @return UpdateInfo if an update is available, null otherwise
     */
    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse =
                httpClient.get("https://api.github.com/repos/rcapraro/Redmine-time-tracking/releases/latest") {
                    header(HttpHeaders.Accept, "application/vnd.github.v3+json")
                    header(HttpHeaders.UserAgent, "RedmineTime/${Version.VERSION}")
                }

            if (response.status == HttpStatusCode.OK) {
                val releaseJson = response.bodyAsText()
                val release = json.decodeFromString<GitHubRelease>(releaseJson)

                val currentVersion = Version.VERSION
                val latestVersion = release.tagName.removePrefix("v")

                if (isNewerVersion(currentVersion, latestVersion)) {
                    val platformAsset = getPlatformAsset(release.assets)
                    UpdateInfo(
                        version = latestVersion,
                        downloadUrl = platformAsset?.browserDownloadUrl,
                        releaseNotes = release.body ?: "",
                        publishedAt = release.publishedAt,
                        fileSize = platformAsset?.size ?: -1L
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            println("Failed to check for updates: ${e.message}")
            null
        }
    }

    /**
     * Downloads the update file to the specified path.
     */
    suspend fun downloadUpdate(
        downloadUrl: String,
        targetPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = java.io.File(targetPath)
            file.parentFile?.mkdirs()

            val response: HttpResponse = httpClient.get(downloadUrl) {
                header(HttpHeaders.Accept, "*/*")
                header(HttpHeaders.UserAgent, "RedmineTime/${Version.VERSION}")
            }

            if (response.status == HttpStatusCode.OK) {
                file.writeBytes(response.body())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Failed to download update: ${e.message}")
            false
        }
    }

    /**
     * Compares two version strings to determine if the second is newer.
     * Supports semantic versioning (e.g., "1.2.3").
     */
    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(currentParts.size, latestParts.size)

        for (i in 0 until maxLength) {
            val currentPart = currentParts.getOrNull(i) ?: 0
            val latestPart = latestParts.getOrNull(i) ?: 0

            when {
                latestPart > currentPart -> return true
                latestPart < currentPart -> return false
            }
        }

        return false
    }

    /**
     * Gets the appropriate asset for the current platform.
     */
    private fun getPlatformAsset(assets: List<GitHubAsset>): GitHubAsset? {
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()

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