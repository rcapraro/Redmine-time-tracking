package com.ps.redmine.lucca

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import java.io.Closeable

const val LUCCA_BASE_URL = "https://packsolutions.ilucca.net"

class LuccaSessionExpiredException : Exception("Lucca session expired")

class LuccaApiException(statusCode: Int, responseBody: String) :
    Exception("Lucca API error: $statusCode - ${responseBody.take(200)}")

class LuccaClient(
    val baseUrl: String = LUCCA_BASE_URL,
    private val httpClient: HttpClient = createHttpClient(),
) : Closeable {

    suspend fun currentUserId(cookies: List<LuccaCookie>): Int =
        get<LuccaDataResponse<LuccaUserDto>>(cookies, "/api/v3/users/me", mapOf("fields" to "id")).data.id

    suspend fun leaves(session: LuccaSession, from: LocalDate, to: LocalDate): List<LuccaLeave> {
        val params = mapOf(
            "leavePeriod.ownerId" to session.userId.toString(),
            "date" to "between,$from,$to",
            "paging" to "0,1000",
            "fields" to "id,date,isAM,isActive,leaveAccount[id,name],leavePeriod[id,isConfirmed]",
        )
        return get<LuccaDataResponse<LuccaItems<LuccaLeaveDto>>>(session.cookies, "/api/v3/leaves", params)
            .data.items
            .filter { it.isActive }
            .map { dto ->
                LuccaLeave(
                    // Lucca may return a date-time ("2026-11-03T00:00:00") where the docs promise a date.
                    date = LocalDate.parse(dto.date.take(10)),
                    isAM = dto.isAM,
                    accountName = dto.leaveAccount?.name.orEmpty(),
                    // An unknown approval status is treated as pending, so the leave is never imported by default.
                    isConfirmed = dto.leavePeriod?.isConfirmed ?: false,
                )
            }
    }

    private suspend inline fun <reified T> get(
        cookies: List<LuccaCookie>,
        path: String,
        params: Map<String, String>,
    ): T {
        val response = httpClient.get(baseUrl + path) {
            header(HttpHeaders.Cookie, cookies.joinToString("; ") { "${it.name}=${it.value}" })
            accept(ContentType.Application.Json)
            params.forEach { (key, value) -> parameter(key, value) }
        }
        val body = response.bodyAsText()
        when {
            response.status.value in 300..399 || response.status == HttpStatusCode.Unauthorized ->
                throw LuccaSessionExpiredException()

            !response.status.isSuccess() -> throw LuccaApiException(response.status.value, body)
        }
        return json.decodeFromString(body)
    }

    override fun close() = httpClient.close()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        private fun createHttpClient() = HttpClient(CIO) {
            // An expired session is answered with a redirect to the login page, not with JSON.
            followRedirects = false
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 10000
            }
        }
    }
}
