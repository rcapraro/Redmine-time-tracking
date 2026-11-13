package com.ps.redmine.lucca

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LuccaClientTest {

    private val cookies = listOf(LuccaCookie("authToken", "abc"), LuccaCookie("other", "x"))
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun client(handler: MockRequestHandler) = LuccaClient(
        baseUrl = "https://lucca.test",
        httpClient = HttpClient(MockEngine) {
            followRedirects = false
            engine { addHandler(handler) }
        },
    )

    @Test
    fun `currentUserId reads the id of the signed-in user`() = runTest {
        val api = client { request ->
            assertEquals("/api/v3/users/me", request.url.encodedPath)
            assertEquals("authToken=abc; other=x", request.headers[HttpHeaders.Cookie])
            respond("""{"header":{},"data":{"id":42,"name":"Jane"}}""", headers = jsonHeaders)
        }

        assertEquals(42, api.currentUserId(cookies))
    }

    @Test
    fun `leaves sends the session cookies and filters the stored user's month`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = client { request ->
            requests += request
            when (request.url.encodedPath) {
                "/api/v3/leaves" -> respond(
                    """
                    {"data":{"items":[
                      {"id":"42-2026-11-03-AM","date":"2026-11-03T00:00:00","isAM":true,"isActive":true,
                       "leaveAccount":{"id":1,"name":"Congés payés"},"leavePeriod":{"id":7,"isConfirmed":true}},
                      {"id":"42-2026-11-04-PM","date":"2026-11-04","isAM":false,"isActive":false,
                       "leaveAccount":{"id":1,"name":"Congés payés"},"leavePeriod":{"id":8,"isConfirmed":true}}
                    ]}}
                    """.trimIndent(),
                    headers = jsonHeaders,
                )

                else -> error("Unexpected ${request.url}")
            }
        }

        val leaves = api.leaves(LuccaSession(42, cookies), LocalDate(2026, 11, 1), LocalDate(2026, 11, 30))

        assertEquals(listOf(LuccaLeave(LocalDate(2026, 11, 3), true, "Congés payés", true)), leaves)
        val request = requests.single()
        assertEquals("authToken=abc; other=x", request.headers[HttpHeaders.Cookie])
        val leavesUrl = request.url
        assertEquals("42", leavesUrl.parameters["leavePeriod.ownerId"])
        assertEquals("between,2026-11-01,2026-11-30", leavesUrl.parameters["date"])
    }

    @Test
    fun `leave without an approval status is treated as pending`() = runTest {
        val api = client {
            respond(
                """{"data":{"items":[{"id":"1","date":"2026-11-03","isAM":true,"leaveAccount":{"id":1,"name":"CP"}}]}}""",
                headers = jsonHeaders,
            )
        }

        val leave = api.leaves(LuccaSession(42, cookies), LocalDate(2026, 11, 1), LocalDate(2026, 11, 30)).single()

        assertEquals(false, leave.isConfirmed)
    }

    @Test
    fun `unauthorized response means the session expired`() = runTest {
        val api = client { respond("", HttpStatusCode.Unauthorized) }

        assertThrows<LuccaSessionExpiredException> { api.currentUserId(cookies) }
    }

    @Test
    fun `redirect to the login page means the session expired`() = runTest {
        val api = client {
            respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://lucca.test/identity/login"))
        }

        assertThrows<LuccaSessionExpiredException> { api.currentUserId(cookies) }
    }

    @Test
    fun `forbidden response is an API error, not an expired session`() = runTest {
        val api = client { respond("", HttpStatusCode.Forbidden) }

        assertThrows<LuccaApiException> { api.currentUserId(cookies) }
    }

    @Test
    fun `server error is reported as an API error`() = runTest {
        val api = client { respond("boom", HttpStatusCode.InternalServerError) }

        assertThrows<LuccaApiException> { api.currentUserId(cookies) }
    }
}
