package com.sliide.usermanager.domain.usecase

import com.sliide.usermanager.domain.model.Result
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observe the list of cached users reactively.
 */
class ObserveUsersUseCase(private val repository: UserRepository) {
    operator fun invoke(): Flow<List<User>> = repository.observeUsers()
}

/**
 * Refresh users from the remote API.
 */
class RefreshUsersUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(): Result<List<User>> = repository.refreshUsers()
}

/**
 * Create a new user after performing domain-level validation.
 */
class CreateUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(name: String, email: String, gender: String): Result<User> {
        return repository.createUser(
            name = name.trim(),
            email = email.trim().lowercase(),
            gender = gender
        )
    }
}

/**
 * Delete a user by ID.
 */
class DeleteUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(userId: Long): Result<Unit> = repository.deleteUser(userId)
}

/**
 * Restore a user to the local cache (undo delete).
 */
class RestoreUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(user: User) = repository.restoreUserLocally(user)
}
