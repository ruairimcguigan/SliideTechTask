package com.sliide.usermanager.domain.usecase

/**
 * Shared validation logic for user input.
 * These run in shared (commonMain) code — no platform dependency.
 */
object ValidationUtils {

    /**
     * Validates a user's name.
     * Rules:
     * - Must not be blank
     * - Must be at least 2 characters
     * - Must be at most 100 characters
     * - Must contain only letters, spaces, hyphens, and apostrophes
     */
    fun validateName(name: String): ValidationResult {
        val trimmed = name.trim()
        return when {
            trimmed.isBlank() -> ValidationResult.Invalid("Name is required")
            trimmed.length < 2 -> ValidationResult.Invalid("Name must be at least 2 characters")
            trimmed.length > 100 -> ValidationResult.Invalid("Name must be under 100 characters")
            !trimmed.matches(NAME_REGEX) -> ValidationResult.Invalid("Name can only contain letters, spaces, hyphens, and apostrophes")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates an email address using RFC 5322 compliant regex.
     */
    fun validateEmail(email: String): ValidationResult {
        val trimmed = email.trim()
        return when {
            trimmed.isBlank() -> ValidationResult.Invalid("Email is required")
            trimmed.length > 254 -> ValidationResult.Invalid("Email is too long")
            !trimmed.matches(EMAIL_REGEX) -> ValidationResult.Invalid("Please enter a valid email address")
            else -> ValidationResult.Valid
        }
    }

    private val NAME_REGEX = Regex("^[\\p{L} '\\-]+$")

    private val EMAIL_REGEX = Regex(
        "^[a-zA-Z0-9.!#\$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,}$"
    )
}

sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()

    val isValid: Boolean get() = this is Valid
}
