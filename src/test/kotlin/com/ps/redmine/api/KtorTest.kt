package com.ps.redmine.api

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class KtorTest {
    @Serializable
    data class TestData(val name: String, val value: Int)

    @Test
    fun testSerialization() {
        // Test that kotlinx-serialization is working
        val json = Json { prettyPrint = true }
        val testData = TestData("test", 123)
        val serialized = json.encodeToString(TestData.serializer(), testData)
        println("[DEBUG_LOG] Serialized: $serialized")

        println("[DEBUG_LOG] Test completed successfully")
    }

    @Test
    fun testSelfSignedCertificateHandling() {
        // Test that we can create a Ktor client with self-signed certificate handling
        try {
            val client = HttpClient(CIO) {
                engine {
                    https {
                        // Use a trust manager that accepts all certificates
                        trustManager = object : X509TrustManager {
                            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                        }
                    }
                }
            }

            // Close the client to release resources
            client.close()

            println("[DEBUG_LOG] Successfully created Ktor client with self-signed certificate handling")
        } catch (e: Exception) {
            println("[DEBUG_LOG] Failed to create Ktor client: ${e.message}")
            throw e
        }
    }
}
