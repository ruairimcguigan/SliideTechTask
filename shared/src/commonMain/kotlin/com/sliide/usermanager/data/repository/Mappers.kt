package com.sliide.usermanager.data.repository

import com.sliide.usermanager.data.api.UserDto
import com.sliide.usermanager.db.UserEntity
import com.sliide.usermanager.domain.model.Gender
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.model.UserStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Mapper functions between data layer types and domain models.
 * Keeps each layer's models independent — changes in the API
 * don't ripple into the UI layer.
 */

// ── DTO → Entity ─────────────────────────────────────────────
fun UserDto.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        name = name,
        email = email,
        gender = gender,
        status = status,
        created_at = Clock.System.now().toEpochMilliseconds()
    )
}

// ── Entity → Domain ──────────────────────────────────────────
fun UserEntity.toDomain(): User {
    return User(
        id = id,
        name = name,
        email = email,
        gender = Gender.fromString(gender),
        status = UserStatus.fromString(status),
        createdAt = Instant.fromEpochMilliseconds(created_at)
    )
}

// ── Domain → Entity (for restoring deleted users) ────────────
fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        name = name,
        email = email,
        gender = gender.name.lowercase(),
        status = status.name.lowercase(),
        created_at = createdAt.toEpochMilliseconds()
    )
}

// ── DTO → Domain (convenience) ───────────────────────────────
fun UserDto.toDomain(): User {
    return toEntity().toDomain()
}
