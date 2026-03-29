package com.sliide.usermanager.data.repository

import com.sliide.usermanager.data.api.CreateUserRequest
import com.sliide.usermanager.data.api.GoRestApiService
import com.sliide.usermanager.data.db.LocalDataSource
import com.sliide.usermanager.domain.model.AppException
import com.sliide.usermanager.domain.model.Result
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.repository.UserRepository
import io.ktor.client.plugins.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offline-first repository implementation.
 *
 * Strategy:
 * - observeUsers() always returns local data (reactive via SQLDelight Flows)
 * - refreshUsers() fetches from network and syncs to local DB
 * - createUser() hits API first, then inserts locally on 201
 * - deleteUser() hits API first, then removes locally on 204
 * - restoreUserLocally() re-inserts without an API call (undo)
 */
class UserRepositoryImpl(
    private val apiService: GoRestApiService,
    private val localDataSource: LocalDataSource
) : UserRepository {

    override fun observeUsers(): Flow<List<User>> {
        return localDataSource.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshUsers(): Result<List<User>> {
        return try {
            val dtos = apiService.getLastPageUsers()
            val entities = dtos.map { it.toEntity() }

            // Replace local cache with fresh data
            localDataSource.deleteAll()
            localDataSource.insertAll(entities)

            val users = entities.map { it.toDomain() }
            Result.Success(users)
        } catch (e: Exception) {
            Result.Error(e.toAppException())
        }
    }

    override suspend fun createUser(
        name: String,
        email: String,
        gender: String
    ): Result<User> {
        return try {
            val request = CreateUserRequest(
                name = name,
                email = email,
                gender = gender
            )
            val dto = apiService.createUser(request)
            val entity = dto.toEntity()
            localDataSource.insert(entity)

            Result.Success(entity.toDomain())
        } catch (e: Exception) {
            Result.Error(e.toAppException())
        }
    }

    override suspend fun deleteUser(userId: Long): Result<Unit> {
        return try {
            val success = apiService.deleteUser(userId)
            if (success) {
                localDataSource.delete(userId)
                Result.Success(Unit)
            } else {
                Result.Error(AppException.ServerError)
            }
        } catch (e: Exception) {
            Result.Error(e.toAppException())
        }
    }

    override suspend fun restoreUserLocally(user: User) {
        localDataSource.insert(user.toEntity())
    }
}

/**
 * Maps platform/library exceptions to typed [AppException].
 */
private fun Exception.toAppException(): AppException {
    return when (this) {
        is HttpRequestTimeoutException -> AppException.NetworkError
        is io.ktor.client.network.sockets.ConnectTimeoutException -> AppException.NetworkError
        else -> {
            val msg = message?.lowercase() ?: ""
            when {
                msg.contains("unable to resolve host") ||
                msg.contains("no address associated") ||
                msg.contains("network") ||
                msg.contains("connect") ||
                msg.contains("timeout") -> AppException.NetworkError
                msg.contains("404") -> AppException.NotFound
                msg.contains("500") || msg.contains("server") -> AppException.ServerError
                else -> AppException.Unknown(message ?: "Unknown error")
            }
        }
    }
}
