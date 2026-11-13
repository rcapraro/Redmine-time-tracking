package com.ps.redmine.lucca

import com.github.javakeyring.BackendNotSupportedException
import com.github.javakeyring.Keyring
import com.github.javakeyring.PasswordAccessException
import kotlinx.serialization.json.Json

class LuccaKeychainException(cause: Throwable) : Exception(cause.message, cause)

internal interface SecretBackend {
    // Null when nothing is stored under the account.
    fun read(account: String): String?
    fun write(account: String, secret: String)
    fun delete(account: String)
}

// Windows Credential Manager caps one entry at 2560 bytes, which a set of Lucca cookies can exceed. The header
// entry holds the chunk count and is written last, so an interrupted write never reads back as a truncated secret.
internal class ChunkedSecret(
    private val backend: SecretBackend,
    private val account: String,
    private val chunkSize: Int = CHUNK_SIZE,
) {
    fun read(): String? {
        val count = chunkCount() ?: return null
        val chunks = (0 until count).map { backend.read(chunkAccount(it)) ?: return null }
        return chunks.joinToString("")
    }

    fun write(secret: String) {
        clear()
        val chunks = secret.chunked(chunkSize)
        chunks.forEachIndexed { index, chunk -> backend.write(chunkAccount(index), chunk) }
        backend.write(account, HEADER_PREFIX + chunks.size)
    }

    fun clear() {
        val count = chunkCount() ?: 0
        backend.delete(account)
        repeat(count) { backend.delete(chunkAccount(it)) }
    }

    private fun chunkCount(): Int? =
        backend.read(account)?.takeIf { it.startsWith(HEADER_PREFIX) }?.removePrefix(HEADER_PREFIX)?.toIntOrNull()

    private fun chunkAccount(index: Int) = "$account.$index"

    private companion object {
        const val HEADER_PREFIX = "chunks:"

        // 1000 chars stay under 2560 bytes even if the backend stores UTF-16.
        const val CHUNK_SIZE = 1000
    }
}

/**
 * Persists the Lucca session in the OS keychain.
 * Calls block on the native keychain; keep them off the UI thread.
 * Every call throws [LuccaKeychainException] when the OS offers no usable keychain.
 */
object LuccaSessionStore {
    private const val SERVICE = "RedmineTime"
    private const val ACCOUNT = "lucca-session"

    /** Returns null when no session is stored, when the keychain refuses access, or when the entry is unreadable. */
    fun load(): LuccaSession? = withSecret { secret ->
        secret.read()?.let { runCatching { Json.decodeFromString<LuccaSession>(it) }.getOrNull() }
    }

    fun save(session: LuccaSession) = withSecret { it.write(Json.encodeToString(session)) }

    fun clear() = withSecret { it.clear() }

    private inline fun <T> withSecret(block: (ChunkedSecret) -> T): T {
        val keyring = try {
            Keyring.create()
        } catch (e: BackendNotSupportedException) {
            throw LuccaKeychainException(e)
        }
        return keyring.use { block(ChunkedSecret(KeyringBackend(it), ACCOUNT)) }
    }

    private class KeyringBackend(private val keyring: Keyring) : SecretBackend {
        override fun read(account: String): String? = try {
            keyring.getPassword(SERVICE, account)
        } catch (_: PasswordAccessException) {
            null
        }

        override fun write(account: String, secret: String) = try {
            keyring.setPassword(SERVICE, account, secret)
        } catch (e: PasswordAccessException) {
            throw LuccaKeychainException(e)
        }

        override fun delete(account: String) = try {
            keyring.deletePassword(SERVICE, account)
        } catch (_: PasswordAccessException) {
            // Nothing stored.
        }
    }
}
