package com.sliide.usermanager.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.sliide.usermanager.ui.screen.UserListScreen
import com.sliide.usermanager.ui.screen.UserListViewModel
import com.sliide.usermanager.ui.theme.SliideTheme
import org.koin.compose.KoinContext
import org.koin.mp.KoinPlatform

/**
 * Root composable — the single entry point for the shared UI.
 * Both Android and iOS call this to render the app.
 */
@Composable
fun App() {
    KoinContext {
        val viewModel = remember { KoinPlatform.getKoin().get<UserListViewModel>() }

        DisposableEffect(Unit) {
            onDispose {
                viewModel.onCleared()
            }
        }

        SliideTheme {
            UserListScreen(viewModel = viewModel)
        }
    }
}
