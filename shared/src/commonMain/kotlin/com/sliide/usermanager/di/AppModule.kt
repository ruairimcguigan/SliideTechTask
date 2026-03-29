package com.sliide.usermanager.di

import com.sliide.usermanager.data.api.GoRestApiService
import com.sliide.usermanager.data.db.LocalDataSource
import com.sliide.usermanager.data.repository.UserRepositoryImpl
import com.sliide.usermanager.domain.repository.UserRepository
import com.sliide.usermanager.domain.usecase.CreateUserUseCase
import com.sliide.usermanager.domain.usecase.DeleteUserUseCase
import com.sliide.usermanager.domain.usecase.ObserveUsersUseCase
import com.sliide.usermanager.domain.usecase.RefreshUsersUseCase
import com.sliide.usermanager.domain.usecase.RestoreUserUseCase
import com.sliide.usermanager.ui.screen.UserListViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Shared Koin module — platform-agnostic dependency graph.
 * Platform modules (Android/iOS) supply the DatabaseDriverFactory.
 */
val sharedModule = module {
    // Data layer
    single { GoRestApiService() }
    single { LocalDataSource(get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get()) }

    // Use cases
    factory { ObserveUsersUseCase(get()) }
    factory { RefreshUsersUseCase(get()) }
    factory { CreateUserUseCase(get()) }
    factory { DeleteUserUseCase(get()) }
    factory { RestoreUserUseCase(get()) }

    // ViewModel
    factory {
        UserListViewModel(
            observeUsers = get(),
            refreshUsers = get(),
            createUser = get(),
            deleteUser = get(),
            restoreUser = get()
        )
    }
}

/**
 * Platform-specific modules supply DatabaseDriverFactory.
 * See androidMain and iosMain for implementations.
 */
expect fun platformModule(): Module
