package com.sliide.usermanager.domain.model

/**
 * A sealed class representing the result of an operation.
 * Used across the domain boundary to communicate success/failure
 * without leaking implementation details (exceptions, HTTP codes).
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: AppException) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): AppException? = (this as? Error)?.exception

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
}

/**
 * Typed application exceptions for structured error handling in the UI layer.
 */
sealed class AppException(override val message: String) : Exception(message) {
    data object NetworkError : AppException("No internet connection. Please check your network.")
    data object ServerError : AppException("Server error. Please try again later.")
    data object NotFound : AppException("Resource not found.")
    data class ValidationError(val field: String, val reason: String) :
        AppException("Validation failed for $field: $reason")
    data class Unknown(override val message: String = "An unexpected error occurred.") :
        AppException(message)
}
