package com.sliide.usermanager.domain.repository

import com.sliide.usermanager.domain.model.Result
import com.sliide.usermanager.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for user operations.
 * Defined in domain, implemented in data layer.
 * Returns Flows for reactive data and Results for one-shot operations.
 */
interface UserRepository {

    /**
     * Observe all cached users as a reactive Flow.
     * Emits whenever the local database changes.
     */
    fun observeUsers(): Flow<List<User>>

    /**
     * Fetch users from the remote API (last page) and cache them locally.
     * Returns Result to indicate success/failure of the network operation.
     */
    suspend fun refreshUsers(): Result<List<User>>

    /**
     * Create a new user via the API and insert into local cache.
     * @return Result containing the created User on success.
     */
    suspend fun createUser(name: String, email: String, gender: String): Result<User>

    /**
     * Delete a user via the API and remove from local cache.
     * @return Result.Success(Unit) on successful deletion.
     */
    suspend fun deleteUser(userId: Long): Result<Unit>

    /**
     * Re-insert a previously deleted user into the local cache only.
     * Used for the "Undo" feature — no API call needed.
     */
    suspend fun restoreUserLocally(user: User)
}
