package com.ps.redmine.lucca

import com.ps.redmine.model.User
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

        val leaves = api.leaves(cookies, 42, LocalDate(2026, 11, 1), LocalDate(2026, 11, 30))

        assertEquals(listOf(LuccaLeave(LocalDate(2026, 11, 3), true, "Congés payés", true)), leaves)
        val request = requests.single()
        assertEquals("authToken=abc; other=x", request.headers[HttpHeaders.Cookie])
        val leavesUrl = request.url
        assertEquals("42", leavesUrl.parameters["leavePeriod.ownerId"])
        assertEquals("between,2026-11-01,2026-11-30", leavesUrl.parameters["date"])
    }

    @Test
    fun `remote work is not an absence`() = runTest {
        val api = client {
            respond(
                """
                {"data":{"items":[
                  {"id":"1","date":"2026-11-03","isAM":true,"isRealLeave":false,
                   "leaveAccount":{"id":9,"name":"Télétravail"},"leavePeriod":{"id":1,"isConfirmed":true}},
                  {"id":"2","date":"2026-11-04","isAM":true,"isRealLeave":true,
                   "leaveAccount":{"id":1,"name":"RTT"},"leavePeriod":{"id":2,"isConfirmed":true}}
                ]}}
                """.trimIndent(),
                headers = jsonHeaders,
            )
        }

        val leaves = api.leaves(cookies, 42, LocalDate(2026, 11, 1), LocalDate(2026, 11, 30))

        assertEquals(listOf("RTT"), leaves.map { it.accountName })
    }

    @Test
    fun `leaves can be read for another person of the planning`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = client { request ->
            requests += request
            respond("""{"data":{"items":[]}}""", headers = jsonHeaders)
        }

        api.leaves(cookies, 77, LocalDate(2026, 11, 1), LocalDate(2026, 11, 30))

        assertEquals("77", requests.single().url.parameters["leavePeriod.ownerId"])
    }

    @Test
    fun `planning people are read page by page until the count is reached`() = runTest {
        val pages = mutableListOf<String?>()
        val api = client { request ->
            assertEquals("/timmi-absences/api/planning/v1.0/users", request.url.encodedPath)
            assertEquals("0", request.url.parameters["population.relativeUsers"])
            val page = request.url.parameters["page"]
            pages += page
            val ids = if (page == "1") 1..500 else 501..502
            val items = ids.joinToString(",") { """{"id":$it,"firstName":"P$it","lastName":"N$it"}""" }
            respond("""{"items":[$items],"count":502}""", headers = jsonHeaders)
        }

        val people = api.planningPeople(cookies)

        assertEquals(listOf("1", "2"), pages)
        assertEquals(502, people.size)
        assertEquals(LuccaPerson(501, "P501", "N501"), people[500])
    }

    private fun directoryPage(ids: Iterable<Int>): String {
        val items = ids.joinToString(",") { """{"id":$it,"firstName":"P$it","lastName":"N$it"}""" }
        return """{"data":{"items":[$items]}}"""
    }

    @Test
    fun `directory people are read by offset until a page brings nobody new`() = runTest {
        val pagings = mutableListOf<String?>()
        val api = client { request ->
            assertEquals("/api/v3/users", request.url.encodedPath)
            assertEquals("id,firstName,lastName", request.url.parameters["fields"])
            assertEquals("id,asc", request.url.parameters["orderBy"])
            val paging = request.url.parameters["paging"]
            pagings += paging
            val ids = when (paging) {
                "0,1000" -> 1..1000
                "1000,1000" -> 1001..1002
                else -> IntRange.EMPTY
            }
            respond(directoryPage(ids), headers = jsonHeaders)
        }

        val people = api.directoryPeople(cookies)

        assertEquals(listOf("0,1000", "1000,1000", "1002,1000"), pagings)
        assertEquals(1002, people.size)
        assertEquals(LuccaPerson(1001, "P1001", "N1001"), people[1000])
    }

    @Test
    fun `a directory capped below the asked page size is still read to the end`() = runTest {
        val api = client { request ->
            val offset = request.url.parameters["paging"].orEmpty().substringBefore(',').toInt()
            respond(directoryPage((offset + 1..250).take(100)), headers = jsonHeaders)
        }

        assertEquals(250, api.directoryPeople(cookies).size)
    }

    @Test
    fun `a directory repeating the same page is read once`() = runTest {
        var calls = 0
        val api = client {
            calls++
            respond(directoryPage(1..1000), headers = jsonHeaders)
        }

        assertEquals(1000, api.directoryPeople(cookies).size)
        assertEquals(2, calls)
    }

    private val jeanJson = """{"id":1,"firstName":"Jean","lastName":"Dupont"}"""
    private val paulJson = """{"id":2,"firstName":"Paul","lastName":"Durand"}"""
    private val jean = LuccaPerson(1, "Jean", "Dupont")
    private val paul = LuccaPerson(2, "Paul", "Durand")
    private val jeanUser = User(10, "Jean", "Dupont", "jdupont")
    private val paulUser = User(11, "Paul", "Durand", "pdurand")

    // A null list stands for one Lucca refuses to give.
    private fun planningAndDirectory(planning: String?, directory: String?) = client { request ->
        val isPlanning = request.url.encodedPath == "/timmi-absences/api/planning/v1.0/users"
        val people = if (isPlanning) planning else directory
        when {
            people == null -> respond("", HttpStatusCode.Forbidden)
            isPlanning -> respond("""{"items":[$people]}""", headers = jsonHeaders)
            else -> respond("""{"data":{"items":[$people]}}""", headers = jsonHeaders)
        }
    }

    @Test
    fun `someone outside the planning is found in the directory`() = runTest {
        val api = planningAndDirectory(planning = jeanJson, directory = "$jeanJson,$paulJson")

        assertEquals(LuccaMatch(paul, inPlanning = false), api.findPerson(cookies, paulUser))
    }

    @Test
    fun `someone of the planning is told to be in it`() = runTest {
        val api = planningAndDirectory(planning = jeanJson, directory = "$jeanJson,$paulJson")

        assertEquals(LuccaMatch(jean, inPlanning = true), api.findPerson(cookies, jeanUser))
    }

    @Test
    fun `a homonym outside the planning leaves nobody picked`() = runTest {
        val homonym = """{"id":9,"firstName":"JEAN","lastName":"DUPONT"}"""
        val api = planningAndDirectory(planning = jeanJson, directory = "$jeanJson,$homonym")

        assertNull(api.findPerson(cookies, jeanUser))
    }

    @Test
    fun `a refused directory still lets a planning colleague be found`() = runTest {
        val api = planningAndDirectory(planning = jeanJson, directory = null)

        assertEquals(LuccaMatch(jean, inPlanning = true), api.findPerson(cookies, jeanUser))
    }

    @Test
    fun `a refused planning still lets someone of the directory be found`() = runTest {
        val api = planningAndDirectory(planning = null, directory = jeanJson)

        assertEquals(LuccaMatch(jean, inPlanning = false), api.findPerson(cookies, jeanUser))
    }

    @Test
    fun `nobody is declared missing from a list Lucca refused`() = runTest {
        val api = planningAndDirectory(planning = jeanJson, directory = null)

        assertThrows<LuccaPeopleUnreadableException> { api.findPerson(cookies, paulUser) }
    }

    @Test
    fun `public holidays on weekdays become confirmed half-day leaves`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val api = client { request ->
            requests += request
            respond(
                """
                [
                  {"date":"2026-11-01T00:00:00","name":"Toussaint","ownerId":77,"am":{"isOff":true},"pm":{"isOff":true}},
                  {"date":"2026-11-11T00:00:00","name":"Armistice","ownerId":77,"am":{"isOff":true},"pm":{"isOff":true}},
                  {"date":"2026-11-18T00:00:00","name":null,"ownerId":77,"am":{"isOff":false},"pm":{"isOff":true}}
                ]
                """.trimIndent(),
                headers = jsonHeaders,
            )
        }

        val holidays = api.publicHolidays(cookies, 77, LocalDate(2026, 11, 1), LocalDate(2026, 11, 30))

        val armistice = LocalDate(2026, 11, 11)
        assertEquals(
            listOf(
                LuccaLeave(armistice, isAM = true, PUBLIC_HOLIDAY_ACCOUNT, isConfirmed = true),
                LuccaLeave(armistice, isAM = false, PUBLIC_HOLIDAY_ACCOUNT, isConfirmed = true),
            ),
            holidays,
        )
        val url = requests.single().url
        assertEquals("/timmi-absences/api/planning/v1.0/userDates", url.encodedPath)
        assertEquals("77", url.parameters["owner.id"])
        assertEquals("2026-11-01", url.parameters["start"])
        assertEquals("2026-11-30", url.parameters["end"])
    }

    @Test
    fun `a public holiday on a half day only gives that half`() = runTest {
        val api = client {
            respond(
                """[{"date":"2026-12-24T00:00:00","name":"Veille de Noël","ownerId":77,"am":{"isOff":false},"pm":{"isOff":true}}]""",
                headers = jsonHeaders,
            )
        }

        val holidays = api.publicHolidays(cookies, 77, LocalDate(2026, 12, 1), LocalDate(2026, 12, 31))

        assertEquals(listOf(LuccaLeave(LocalDate(2026, 12, 24), isAM = false, PUBLIC_HOLIDAY_ACCOUNT, true)), holidays)
    }

    @Test
    fun `leave without an approval status is treated as pending`() = runTest {
        val api = client {
            respond(
                """{"data":{"items":[{"id":"1","date":"2026-11-03","isAM":true,"leaveAccount":{"id":1,"name":"CP"}}]}}""",
                headers = jsonHeaders,
            )
        }

        val leave = api.leaves(cookies, 42, LocalDate(2026, 11, 1), LocalDate(2026, 11, 30)).single()

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
