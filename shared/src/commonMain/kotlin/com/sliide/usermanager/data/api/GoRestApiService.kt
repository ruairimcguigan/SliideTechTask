package com.sliide.usermanager.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Ktor-based API client for the GoRest public API.
 * Handles all HTTP communication and JSON serialization.
 */
class GoRestApiService {

    companion object {
        private const val BASE_URL = "https://gorest.co.in/public/v2"
        // GoRest requires an auth token for write operations.
        // In production, this would be injected via BuildConfig / environment.
        private const val AUTH_TOKEN = "Bearer b5e61d05d63de4b2f1011fc68ff06e3cf316cca5200a94a326443018ef44a172"
    }

    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                coerceInputValues = true
                encodeDefaults = true
            })
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
        }

        defaultRequest {
            header(HttpHeaders.Authorization, AUTH_TOKEN)
            contentType(ContentType.Application.Json)
        }
    }

    /**
     * Fetches users from a specific page.
     * GoRest returns pagination info in response headers:
     *   x-pagination-total, x-pagination-pages, x-pagination-page
     */
    suspend fun getUsers(page: Int? = null): Pair<List<UserDto>, PaginationInfo> {
        val response: HttpResponse = client.get("$BASE_URL/users") {
            if (page != null) {
                parameter("page", page)
            }
            parameter("per_page", 20)
        }

        val users: List<UserDto> = response.body()

        val pagination = PaginationInfo(
            totalPages = response.headers["x-pagination-pages"]?.toIntOrNull() ?: 1,
            currentPage = response.headers["x-pagination-page"]?.toIntOrNull() ?: 1,
            totalRecords = response.headers["x-pagination-total"]?.toIntOrNull() ?: 0
        )

        return users to pagination
    }

    /**
     * Fetches users from the last available page.
     * First calls page 1 to get total pages, then fetches the last page.
     */
    suspend fun getLastPageUsers(): List<UserDto> {
        val (_, pagination) = getUsers(page = 1)
        if (pagination.totalPages <= 1) {
            val (users, _) = getUsers(page = 1)
            return users
        }
        val (lastPageUsers, _) = getUsers(page = pagination.totalPages)
        return lastPageUsers
    }

    /**
     * Create a new user.
     * @return The created [UserDto] (HTTP 201 on success).
     */
    suspend fun createUser(request: CreateUserRequest): UserDto {
        val response: HttpResponse = client.post("$BASE_URL/users") {
            setBody(request)
        }

        if (response.status != HttpStatusCode.Created) {
            val errorMessage = try {
                val errors: List<ApiError> = response.body()
                errors.joinToString("; ") { "${it.field}: ${it.message}" }
            } catch (_: Exception) {
                "HTTP ${response.status.value}"
            }
            throw Exception(errorMessage)
        }

        return response.body()
    }

    /**
     * Delete a user by ID.
     * @return true if successful (HTTP 204) or already gone (HTTP 404).
     */
    suspend fun deleteUser(userId: Long): Boolean {
        val response: HttpResponse = client.delete("$BASE_URL/users/$userId")
        return response.status == HttpStatusCode.NoContent ||
            response.status == HttpStatusCode.NotFound
    }
}
