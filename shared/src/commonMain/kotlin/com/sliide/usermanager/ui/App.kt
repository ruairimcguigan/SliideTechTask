package com.sliide.usermanager.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.sliide.usermanager.ui.screen.UserListScreen
import com.sliide.usermanager.ui.screen.UserListViewModel
import com.sliide.usermanager.ui.theme.SliideTheme
import org.koin.compose.koinInject

/**
 * Root composable — the single entry point for the shared UI.
 * Both Android and iOS call this to render the app.
 */
@Composable
fun App() {
    SliideTheme {
        val viewModel: UserListViewModel = koinInject()

        DisposableEffect(Unit) {
            onDispose {
                viewModel.onCleared()
            }
        }

        UserListScreen(viewModel = viewModel)
    }
}
