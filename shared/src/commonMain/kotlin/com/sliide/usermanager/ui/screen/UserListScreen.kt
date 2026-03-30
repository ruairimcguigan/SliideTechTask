package com.sliide.usermanager.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sliide.usermanager.ui.components.AddUserDialog
import com.sliide.usermanager.ui.components.DeleteConfirmDialog
import com.sliide.usermanager.ui.components.EmptyState
import com.sliide.usermanager.ui.components.ErrorState
import com.sliide.usermanager.ui.components.ShimmerUserList
import com.sliide.usermanager.ui.components.SwipeableUserCard
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
    val listState = rememberLazyListState()
    var highlightedUserId by remember { mutableStateOf<Long?>(null) }
    var isWideScreen by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Hide FAB when scrolling down, show when scrolling up
    var previousScrollIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }
    val isFabVisible by remember {
        derivedStateOf {
            val currentIndex = listState.firstVisibleItemIndex
            val currentOffset = listState.firstVisibleItemScrollOffset
            val scrollingDown = currentIndex > previousScrollIndex ||
                (currentIndex == previousScrollIndex && currentOffset > previousScrollOffset)
            previousScrollIndex = currentIndex
            previousScrollOffset = currentOffset
            !scrollingDown || currentIndex == 0
        }
    }

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
                    listState.animateScrollToItem(0)
                    highlightedUserId = effect.userId
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
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = slideInVertically(initialOffsetY = { it * 2 }),
                exit = slideOutVertically(targetOffsetY = { it * 2 })
            ) {
                FloatingActionButton(
                    onClick = { viewModel.onIntent(UserListIntent.ShowAddUserDialog) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add user",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
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
                isWideScreen = maxWidth > 600.dp

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
                                listState = listState,
                                highlightedUserId = highlightedUserId,
                                onHighlightConsumed = { highlightedUserId = null },
                                onRefresh = { viewModel.onIntent(UserListIntent.RefreshUsers) },
                                onUserClick = { viewModel.onIntent(UserListIntent.SelectUser(it)) },
                                onUserLongClick = { viewModel.onIntent(UserListIntent.RequestDelete(it)) },
                                onUserSwipeDelete = { viewModel.onIntent(UserListIntent.RequestDelete(it)) },
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
                        listState = listState,
                        highlightedUserId = highlightedUserId,
                        onHighlightConsumed = { highlightedUserId = null },
                        onRefresh = { viewModel.onIntent(UserListIntent.RefreshUsers) },
                        onUserClick = { viewModel.onIntent(UserListIntent.SelectUser(it)) },
                        onUserLongClick = { viewModel.onIntent(UserListIntent.RequestDelete(it)) },
                        onUserSwipeDelete = { viewModel.onIntent(UserListIntent.RequestDelete(it)) },
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

    // Portrait bottom sheet for user details
    if (!isWideScreen && state.selectedUser != null && state.deleteConfirmUser == null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onIntent(UserListIntent.SelectUser(null)) },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            UserDetailPanel(
                user = state.selectedUser,
                onDeleteClick = {
                    viewModel.onIntent(UserListIntent.RequestDelete(it))
                }
            )
        }
    }
}

/**
 * The scrollable user list content, shared between portrait and master-detail modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserListContent(
    state: UserListState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    highlightedUserId: Long?,
    onHighlightConsumed: () -> Unit,
    onRefresh: () -> Unit,
    onUserClick: (com.sliide.usermanager.domain.model.User) -> Unit,
    onUserLongClick: (com.sliide.usermanager.domain.model.User) -> Unit,
    onUserSwipeDelete: (com.sliide.usermanager.domain.model.User) -> Unit,
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
                    state = listState,
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
                        val index = state.users.indexOf(user)

                        // Staggered entry: each card fades and slides in with a slight delay
                        val animProgress = remember { Animatable(0f) }
                        LaunchedEffect(user.id) {
                            kotlinx.coroutines.delay(index.coerceAtMost(10) * 50L)
                            animProgress.animateTo(1f, animationSpec = tween(350))
                        }

                        // Shake animation for newly created user
                        val shakeOffset = remember { Animatable(0f) }
                        val isHighlighted = highlightedUserId == user.id
                        LaunchedEffect(isHighlighted) {
                            if (isHighlighted) {
                                kotlinx.coroutines.delay(200) // wait for scroll to settle
                                repeat(3) {
                                    shakeOffset.animateTo(8f, animationSpec = tween(60))
                                    shakeOffset.animateTo(-8f, animationSpec = tween(60))
                                }
                                shakeOffset.animateTo(0f, animationSpec = tween(60))
                                onHighlightConsumed()
                            }
                        }

                        SwipeableUserCard(
                            user = user,
                            isSelected = state.selectedUser?.id == user.id,
                            showSwipeHint = index == 0 && highlightedUserId == null,
                            onClick = { onUserClick(user) },
                            onLongClick = { onUserLongClick(user) },
                            onDelete = { onUserSwipeDelete(user) },
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = animProgress.value
                                    translationY = (1f - animProgress.value) * 40f
                                    translationX = shakeOffset.value
                                }
                                .animateItem(
                                    fadeInSpec = tween(300),
                                    fadeOutSpec = tween(200)
                                )
                        )
                    }
                }
            }
        }
    }
}
