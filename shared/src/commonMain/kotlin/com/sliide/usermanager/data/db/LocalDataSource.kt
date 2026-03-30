package com.sliide.usermanager.data.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sliide.usermanager.db.UserDatabase
import com.sliide.usermanager.db.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * Local data source wrapping SQLDelight-generated queries.
 * Provides reactive observation via Flows and CRUD operations.
 */
class LocalDataSource(driverFactory: DatabaseDriverFactory) {

    private val database = UserDatabase(driverFactory.createDriver())
    private val queries = database.userEntityQueries

    /**
     * Observe all cached users as a reactive Flow.
     * Emits automatically whenever the underlying table changes.
     */
    fun observeAll(): Flow<List<UserEntity>> {
        return queries.getAllUsers()
            .asFlow()
            .mapToList(Dispatchers.Main)
    }

    fun getAll(): List<UserEntity> {
        return queries.getAllUsers().executeAsList()
    }

    fun getById(id: Long): UserEntity? {
        return queries.getUserById(id).executeAsOneOrNull()
    }

    fun insert(entity: UserEntity) {
        queries.insertUser(
            id = entity.id,
            name = entity.name,
            email = entity.email,
            gender = entity.gender,
            status = entity.status,
            created_at = entity.created_at
        )
    }

    fun insertAll(entities: List<UserEntity>) {
        database.transaction {
            entities.forEach { entity ->
                queries.insertUser(
                    id = entity.id,
                    name = entity.name,
                    email = entity.email,
                    gender = entity.gender,
                    status = entity.status,
                    created_at = entity.created_at
                )
            }
        }
    }

    fun delete(id: Long) {
        queries.deleteUser(id)
    }

    fun deleteAll() {
        queries.deleteAllUsers()
    }
}
