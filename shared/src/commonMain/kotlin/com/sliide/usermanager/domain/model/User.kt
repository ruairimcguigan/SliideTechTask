package com.sliide.usermanager.domain.model

import kotlinx.datetime.Instant

/**
 * Core domain entity representing a user in the system.
 * This is the single source of truth — UI and data layers map to/from this.
 */
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val gender: Gender,
    val status: UserStatus,
    val createdAt: Instant
)

enum class Gender {
    MALE, FEMALE;

    companion object {
        fun fromString(value: String): Gender =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MALE
    }
}

enum class UserStatus {
    ACTIVE, INACTIVE;

    companion object {
        fun fromString(value: String): UserStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ACTIVE
    }
}
