package com.sliide.usermanager.ui.screen

import com.sliide.usermanager.domain.model.AppException
import com.sliide.usermanager.domain.model.Result
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.usecase.CreateUserUseCase
import com.sliide.usermanager.domain.usecase.DeleteUserUseCase
import com.sliide.usermanager.domain.usecase.ObserveUsersUseCase
import com.sliide.usermanager.domain.usecase.RefreshUsersUseCase
import com.sliide.usermanager.domain.usecase.RestoreUserUseCase
import com.sliide.usermanager.domain.usecase.ValidationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── MVI State ───────────────────────────────────────────────

data class UserListState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isAddUserDialogVisible: Boolean = false,
    val isCreatingUser: Boolean = false,
    val selectedUser: User? = null, // For master-detail
    val deleteConfirmUser: User? = null, // User pending delete confirmation
    // Form fields
    val formName: String = "",
    val formEmail: String = "",
    val formGender: String = "male",
    val nameError: String? = null,
    val emailError: String? = null
)

// ─── MVI Side Effects ────────────────────────────────────────

sealed class UserListEffect {
    data class ShowSnackbar(val message: String) : UserListEffect()
    data class ShowUndoSnackbar(val user: User, val message: String) : UserListEffect()
    data object UserCreatedSuccess : UserListEffect()
}

// ─── MVI Intents ─────────────────────────────────────────────

sealed class UserListIntent {
    data object LoadUsers : UserListIntent()
    data object RefreshUsers : UserListIntent()
    data object ShowAddUserDialog : UserListIntent()
    data object DismissAddUserDialog : UserListIntent()
    data class UpdateFormName(val name: String) : UserListIntent()
    data class UpdateFormEmail(val email: String) : UserListIntent()
    data class UpdateFormGender(val gender: String) : UserListIntent()
    data object SubmitNewUser : UserListIntent()
    data class SelectUser(val user: User?) : UserListIntent()
    data class RequestDelete(val user: User) : UserListIntent()
    data object ConfirmDelete : UserListIntent()
    data object CancelDelete : UserListIntent()
    data class UndoDelete(val user: User) : UserListIntent()
    data object DismissError : UserListIntent()
}

/**
 * Shared ViewModel following the MVI pattern.
 *
 * - State: Single immutable [UserListState] exposed via StateFlow
 * - Intents: User actions dispatched via [onIntent]
 * - Effects: One-shot events (Snackbar, navigation) via SharedFlow
 *
 * Lifecycle is managed by the platform (Android: tied to Activity/Fragment,
 * iOS: tied to SwiftUI view lifecycle via manual scope management).
 */
class UserListViewModel(
    private val observeUsers: ObserveUsersUseCase,
    private val refreshUsers: RefreshUsersUseCase,
    private val createUser: CreateUserUseCase,
    private val deleteUser: DeleteUserUseCase,
    private val restoreUser: RestoreUserUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(UserListState())
    val state: StateFlow<UserListState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<UserListEffect>(extraBufferCapacity = 10)
    val effects = _effects.asSharedFlow()

    init {
        observeLocalUsers()
        onIntent(UserListIntent.LoadUsers)
    }

    fun onIntent(intent: UserListIntent) {
        when (intent) {
            is UserListIntent.LoadUsers -> loadUsers()
            is UserListIntent.RefreshUsers -> refresh()
            is UserListIntent.ShowAddUserDialog -> showAddDialog()
            is UserListIntent.DismissAddUserDialog -> dismissAddDialog()
            is UserListIntent.UpdateFormName -> updateName(intent.name)
            is UserListIntent.UpdateFormEmail -> updateEmail(intent.email)
            is UserListIntent.UpdateFormGender -> updateGender(intent.gender)
            is UserListIntent.SubmitNewUser -> submitNewUser()
            is UserListIntent.SelectUser -> selectUser(intent.user)
            is UserListIntent.RequestDelete -> requestDelete(intent.user)
            is UserListIntent.ConfirmDelete -> confirmDelete()
            is UserListIntent.CancelDelete -> cancelDelete()
            is UserListIntent.UndoDelete -> undoDelete(intent.user)
            is UserListIntent.DismissError -> dismissError()
        }
    }

    /**
     * Observe local DB changes reactively.
     * Any insert/delete in SQLDelight triggers a new emission here.
     */
    private fun observeLocalUsers() {
        scope.launch {
            observeUsers().collect { users ->
                _state.update { it.copy(users = users) }
            }
        }
    }

    private fun loadUsers() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = refreshUsers()) {
                is Result.Success -> _state.update { it.copy(isLoading = false) }
                is Result.Error -> _state.update {
                    it.copy(
                        isLoading = false,
                        error = result.exception.message
                    )
                }
            }
        }
    }

    private fun refresh() {
        scope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            when (val result = refreshUsers()) {
                is Result.Success -> _state.update { it.copy(isRefreshing = false) }
                is Result.Error -> {
                    _state.update { it.copy(isRefreshing = false) }
                    _effects.emit(UserListEffect.ShowSnackbar(result.exception.message))
                }
            }
        }
    }

    // ─── Add User Form ───────────────────────────────────────

    private fun showAddDialog() {
        _state.update {
            it.copy(
                isAddUserDialogVisible = true,
                formName = "",
                formEmail = "",
                formGender = "male",
                nameError = null,
                emailError = null
            )
        }
    }

    private fun dismissAddDialog() {
        _state.update { it.copy(isAddUserDialogVisible = false) }
    }

    private fun updateName(name: String) {
        val validation = ValidationUtils.validateName(name)
        _state.update {
            it.copy(
                formName = name,
                nameError = if (name.isBlank()) null
                else (validation as? com.sliide.usermanager.domain.usecase.ValidationResult.Invalid)?.message
            )
        }
    }

    private fun updateEmail(email: String) {
        val validation = ValidationUtils.validateEmail(email)
        _state.update {
            it.copy(
                formEmail = email,
                emailError = if (email.isBlank()) null
                else (validation as? com.sliide.usermanager.domain.usecase.ValidationResult.Invalid)?.message
            )
        }
    }

    private fun updateGender(gender: String) {
        _state.update { it.copy(formGender = gender) }
    }

    private fun submitNewUser() {
        val currentState = _state.value
        val nameValidation = ValidationUtils.validateName(currentState.formName)
        val emailValidation = ValidationUtils.validateEmail(currentState.formEmail)

        if (!nameValidation.isValid || !emailValidation.isValid) {
            _state.update {
                it.copy(
                    nameError = (nameValidation as? com.sliide.usermanager.domain.usecase.ValidationResult.Invalid)?.message,
                    emailError = (emailValidation as? com.sliide.usermanager.domain.usecase.ValidationResult.Invalid)?.message
                )
            }
            return
        }

        scope.launch {
            _state.update { it.copy(isCreatingUser = true) }
            when (val result = createUser(
                currentState.formName,
                currentState.formEmail,
                currentState.formGender
            )) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isCreatingUser = false,
                            isAddUserDialogVisible = false
                        )
                    }
                    _effects.emit(UserListEffect.UserCreatedSuccess)
                    _effects.emit(UserListEffect.ShowSnackbar("User created successfully!"))
                }
                is Result.Error -> {
                    _state.update { it.copy(isCreatingUser = false, isAddUserDialogVisible = false) }
                    _effects.emit(UserListEffect.ShowSnackbar(result.exception.message))
                }
            }
        }
    }

    // ─── Delete with Undo ────────────────────────────────────

    private fun selectUser(user: User?) {
        _state.update { it.copy(selectedUser = user) }
    }

    private fun requestDelete(user: User) {
        _state.update { it.copy(deleteConfirmUser = user) }
    }

    private fun cancelDelete() {
        _state.update { it.copy(deleteConfirmUser = null) }
    }

    private fun confirmDelete() {
        val user = _state.value.deleteConfirmUser ?: return
        _state.update { it.copy(deleteConfirmUser = null) }

        scope.launch {
            when (val result = deleteUser(user.id)) {
                is Result.Success -> {
                    // Clear selection if deleted user was selected
                    if (_state.value.selectedUser?.id == user.id) {
                        _state.update { it.copy(selectedUser = null) }
                    }
                    _effects.emit(
                        UserListEffect.ShowUndoSnackbar(
                            user = user,
                            message = "${user.name} deleted"
                        )
                    )
                }
                is Result.Error -> {
                    _effects.emit(UserListEffect.ShowSnackbar("Failed to delete: ${result.exception.message}"))
                }
            }
        }
    }

    private fun undoDelete(user: User) {
        scope.launch {
            restoreUser(user)
            _effects.emit(UserListEffect.ShowSnackbar("${user.name} restored"))
        }
    }

    private fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun onCleared() {
        scope.cancel()
    }
}
