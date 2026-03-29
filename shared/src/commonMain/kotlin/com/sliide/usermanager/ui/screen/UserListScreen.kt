package com.sliide.usermanager.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sliide.usermanager.ui.components.AddUserDialog
import com.sliide.usermanager.ui.components.DeleteConfirmDialog
import com.sliide.usermanager.ui.components.EmptyState
import com.sliide.usermanager.ui.components.ErrorState
import com.sliide.usermanager.ui.components.ShimmerUserList
import com.sliide.usermanager.ui.components.UserCard
import com.sliide.usermanager.ui.components.UserDetailPanel

/**
 * The root screen composable. Adapts between:
 * - Portrait/Phone: Single-column user list
 * - Landscape/Tablet (>600dp width): Master-detail two-column layout
 *
 * Uses Material 3 components, pull-to-refresh, FAB, and Snackbar with undo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(viewModel: UserListViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Collect side effects
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UserListEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is UserListEffect.ShowUndoSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = effect.message,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(UserListIntent.UndoDelete(effect.user))
                    }
                }
                is UserListEffect.UserCreatedSuccess -> {
                    // Handled by ShowSnackbar effect
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "User Directory",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.users.isNotEmpty()) {
                            Text(
                                text = "${state.users.size} users",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onIntent(UserListIntent.ShowAddUserDialog) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // Adaptive layout based on screen width
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth > 600.dp

                if (isWideScreen) {
                    // Master-Detail layout
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Master (list) pane
                        Box(
                            modifier = Modifier
                                .weight(0.4f)
                                .fillMaxHeight()
                        ) {
                            UserListContent(
                                state = state,
                                onRefresh = { viewModel.onIntent(UserListIntent.RefreshUsers) },
                                onUserClick = { viewModel.onIntent(UserListIntent.SelectUser(it)) },
                                onUserLongClick = { viewModel.onIntent(UserListIntent.RequestDelete(it)) },
                                onRetry = { viewModel.onIntent(UserListIntent.LoadUsers) }
                            )
                        }

                        // Divider
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Detail pane
                        Box(
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxHeight()
                        ) {
                            UserDetailPanel(
                                user = state.selectedUser,
                                onDeleteClick = { viewModel.onIntent(UserListIntent.RequestDelete(it)) }
                            )
                        }
                    }
                } else {
                    // Single column (portrait)
                    UserListContent(
                        state = state,
                        onRefresh = { viewModel.onIntent(UserListIntent.RefreshUsers) },
                        onUserClick = { viewModel.onIntent(UserListIntent.SelectUser(it)) },
                        onUserLongClick = { viewModel.onIntent(UserListIntent.RequestDelete(it)) },
                        onRetry = { viewModel.onIntent(UserListIntent.LoadUsers) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (state.isAddUserDialogVisible) {
        AddUserDialog(
            name = state.formName,
            email = state.formEmail,
            gender = state.formGender,
            nameError = state.nameError,
            emailError = state.emailError,
            isCreating = state.isCreatingUser,
            onNameChange = { viewModel.onIntent(UserListIntent.UpdateFormName(it)) },
            onEmailChange = { viewModel.onIntent(UserListIntent.UpdateFormEmail(it)) },
            onGenderChange = { viewModel.onIntent(UserListIntent.UpdateFormGender(it)) },
            onSubmit = { viewModel.onIntent(UserListIntent.SubmitNewUser) },
            onDismiss = { viewModel.onIntent(UserListIntent.DismissAddUserDialog) }
        )
    }

    state.deleteConfirmUser?.let { user ->
        DeleteConfirmDialog(
            userName = user.name,
            onConfirm = { viewModel.onIntent(UserListIntent.ConfirmDelete) },
            onDismiss = { viewModel.onIntent(UserListIntent.CancelDelete) }
        )
    }
}

/**
 * The scrollable user list content, shared between portrait and master-detail modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserListContent(
    state: UserListState,
    onRefresh: () -> Unit,
    onUserClick: (com.sliide.usermanager.domain.model.User) -> Unit,
    onUserLongClick: (com.sliide.usermanager.domain.model.User) -> Unit,
    onRetry: () -> Unit
) {
    when {
        // Initial loading
        state.isLoading && state.users.isEmpty() -> {
            ShimmerUserList()
        }

        // Error with no cached data
        state.error != null && state.users.isEmpty() -> {
            ErrorState(
                message = state.error,
                onRetry = onRetry
            )
        }

        // Empty state
        !state.isLoading && state.users.isEmpty() -> {
            EmptyState()
        }

        // Data loaded (possibly refreshing)
        else -> {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 88.dp // Space for FAB
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.users,
                        key = { it.id }
                    ) { user ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + slideInVertically(tween(300)),
                            exit = fadeOut(tween(200))
                        ) {
                            UserCard(
                                user = user,
                                isSelected = state.selectedUser?.id == user.id,
                                onClick = { onUserClick(user) },
                                onLongClick = { onUserLongClick(user) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}
