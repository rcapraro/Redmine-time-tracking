package com.ps.redmine.lucca

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChunkedSecretTest {

    private class InMemoryBackend : SecretBackend {
        val entries = mutableMapOf<String, String>()
        override fun read(account: String) = entries[account]
        override fun write(account: String, secret: String) {
            entries[account] = secret
        }

        override fun delete(account: String) {
            entries.remove(account)
        }
    }

    private val backend = InMemoryBackend()
    private val secret = ChunkedSecret(backend, "session", chunkSize = 4)

    @Test
    fun `a long secret is split into entries no larger than the chunk size and read back whole`() {
        secret.write("abcdefghij")

        assertEquals("abcdefghij", secret.read())
        assertEquals(listOf("abcd", "efgh", "ij"), listOf("session.0", "session.1", "session.2").map { backend.entries[it] })
        assertTrue(backend.entries.filterKeys { it != "session" }.values.all { it.length <= 4 })
    }

    @Test
    fun `rewriting with a shorter secret leaves no stale chunk behind`() {
        secret.write("abcdefghij")
        secret.write("xy")

        assertEquals("xy", secret.read())
        assertEquals(setOf("session", "session.0"), backend.entries.keys)
    }

    @Test
    fun `clear removes the header and every chunk`() {
        secret.write("abcdefghij")
        secret.clear()

        assertNull(secret.read())
        assertTrue(backend.entries.isEmpty())
    }

    @Test
    fun `a missing chunk reads as no secret rather than a truncated one`() {
        secret.write("abcdefghij")
        backend.entries.remove("session.1")

        assertNull(secret.read())
    }

    @Test
    fun `an entry in the former single-entry format reads as no secret`() {
        backend.entries["session"] = """{"userId":1,"cookies":[]}"""

        assertNull(secret.read())
    }
}
