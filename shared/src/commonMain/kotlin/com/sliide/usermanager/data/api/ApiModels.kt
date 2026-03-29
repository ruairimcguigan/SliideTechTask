package com.sliide.usermanager.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO representing a user from the GoRest API.
 * Mapped to domain [User] via extension functions.
 */
@Serializable
data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val gender: String,
    val status: String
)

/**
 * Request body for creating a new user.
 */
@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String,
    val gender: String,
    val status: String = "active"
)

/**
 * Error response from GoRest API (e.g., 422 validation errors).
 * GoRest returns an array of these: [{"field":"email","message":"has already been taken"}]
 */
@Serializable
data class ApiError(
    val field: String = "",
    val message: String = ""
)

/**
 * Pagination metadata from GoRest response headers.
 */
data class PaginationInfo(
    val totalPages: Int,
    val currentPage: Int,
    val totalRecords: Int
)
