package com.sliide.usermanager

import com.sliide.usermanager.domain.model.AppException
import com.sliide.usermanager.domain.model.Gender
import com.sliide.usermanager.domain.model.Result
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.model.UserStatus
import com.sliide.usermanager.domain.repository.UserRepository
import com.sliide.usermanager.domain.usecase.CreateUserUseCase
import com.sliide.usermanager.domain.usecase.DeleteUserUseCase
import com.sliide.usermanager.domain.usecase.ObserveUsersUseCase
import com.sliide.usermanager.domain.usecase.RefreshUsersUseCase
import com.sliide.usermanager.domain.usecase.RestoreUserUseCase
import com.sliide.usermanager.ui.screen.UserListEffect
import com.sliide.usermanager.ui.screen.UserListIntent
import com.sliide.usermanager.ui.screen.UserListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeUserRepository
    private lateinit var viewModel: UserListViewModel

    private val testUser = User(
        id = 1L,
        name = "John Doe",
        email = "john@example.com",
        gender = Gender.MALE,
        status = UserStatus.ACTIVE,
        createdAt = Clock.System.now()
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeUserRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        if (::viewModel.isInitialized) {
            viewModel.onCleared()
        }
    }

    private fun createViewModel(): UserListViewModel {
        return UserListViewModel(
            observeUsers = ObserveUsersUseCase(fakeRepository),
            refreshUsers = RefreshUsersUseCase(fakeRepository),
            createUser = CreateUserUseCase(fakeRepository),
            deleteUser = DeleteUserUseCase(fakeRepository),
            restoreUser = RestoreUserUseCase(fakeRepository)
        )
    }

    @Test
    fun initialState_isLoading() = runTest {
        fakeRepository.refreshResult = Result.Success(emptyList())
        viewModel = createViewModel()
        // Initial state before any coroutines complete
        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun loadUsers_success_updatesState() = runTest {
        fakeRepository.refreshResult = Result.Success(listOf(testUser))
        fakeRepository.usersFlow.value = listOf(testUser)
        viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(1, state.users.size)
        assertEquals("John Doe", state.users.first().name)
    }

    @Test
    fun loadUsers_networkError_showsError() = runTest {
        fakeRepository.refreshResult = Result.Error(AppException.NetworkError)
        viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.error != null)
        assertTrue(state.error!!.contains("internet", ignoreCase = true))
    }

    @Test
    fun showAddUserDialog_updatesState() = runTest {
        fakeRepository.refreshResult = Result.Success(emptyList())
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(UserListIntent.ShowAddUserDialog)

        assertTrue(viewModel.state.value.isAddUserDialogVisible)
        assertEquals("", viewModel.state.value.formName)
        assertEquals("", viewModel.state.value.formEmail)
        assertEquals("male", viewModel.state.value.formGender)
    }

    @Test
    fun dismissAddUserDialog_updatesState() = runTest {
        fakeRepository.refreshResult = Result.Success(emptyList())
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(UserListIntent.ShowAddUserDialog)
        viewModel.onIntent(UserListIntent.DismissAddUserDialog)

        assertFalse(viewModel.state.value.isAddUserDialogVisible)
    }

    @Test
    fun updateFormName_validatesInRealTime() = runTest {
        fakeRepository.refreshResult = Result.Success(emptyList())
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(UserListIntent.UpdateFormName("J"))
        assertTrue(viewModel.state.value.nameError != null)

        viewModel.onIntent(UserListIntent.UpdateFormName("John"))
        assertNull(viewModel.state.value.nameError)
    }

    @Test
    fun updateFormEmail_validatesInRealTime() = runTest {
        fakeRepository.refreshResult = Result.Success(emptyList())
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(UserListIntent.UpdateFormEmail("invalid"))
        assertTrue(viewModel.state.value.emailError != null)

        viewModel.onIntent(UserListIntent.UpdateFormEmail("valid@example.com"))
        assertNull(viewModel.state.value.emailError)
    }

    @Test
    fun submitNewUser_withInvalidFields_showsErrors() = runTest {
        fakeRepository.refreshResult = Result.Success(emptyList())
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(UserListIntent.ShowAddUserDialog)
        viewModel.onIntent(UserListIntent.SubmitNewUser)

        assertTrue(viewModel.state.value.nameError != null)
        assertTrue(viewModel.state.value.emailError != null)
    }

    @Test
    fun submitNewUser_success_closesDialog() = runTest {
        fakeRepository.refreshResult = Result.Success(emptyList())
        fakeRepository.createResult = Result.Success(testUser)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(UserListIntent.ShowAddUserDialog)
        viewModel.onIntent(UserListIntent.UpdateFormName("John Doe"))
        viewModel.onIntent(UserListIntent.UpdateFormEmail("john@example.com"))
        viewModel.onIntent(UserListIntent.SubmitNewUser)

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isAddUserDialogVisible)
        assertFalse(viewModel.state.value.isCreatingUser)
    }

    @Test
    fun requestDelete_showsConfirmation() = runTest {
        fakeRepository.refreshResult = Result.Success(listOf(testUser))
        fakeRepository.usersFlow.value = listOf(testUser)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(UserListIntent.RequestDelete(testUser))

        assertEquals(testUser, viewModel.state.value.deleteConfirmUser)
    }

    @Test
    fun cancelDelete_clearsConfirmation() = runTest {
        fakeRepository.refreshResult = Result.Success(listOf(testUser))
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(UserListIntent.RequestDelete(testUser))
        viewModel.onIntent(UserListIntent.CancelDelete)

        assertNull(viewModel.state.value.deleteConfirmUser)
    }

    @Test
    fun selectUser_updatesSelectedUser() = runTest {
        fakeRepository.refreshResult = Result.Success(listOf(testUser))
        fakeRepository.usersFlow.value = listOf(testUser)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(UserListIntent.SelectUser(testUser))

        assertEquals(testUser, viewModel.state.value.selectedUser)
    }

    @Test
    fun selectUser_null_clearsSelection() = runTest {
        fakeRepository.refreshResult = Result.Success(listOf(testUser))
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(UserListIntent.SelectUser(testUser))
        viewModel.onIntent(UserListIntent.SelectUser(null))

        assertNull(viewModel.state.value.selectedUser)
    }

    @Test
    fun dismissError_clearsError() = runTest {
        fakeRepository.refreshResult = Result.Error(AppException.NetworkError)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(UserListIntent.DismissError)

        assertNull(viewModel.state.value.error)
    }
}

// ─── Fake Repository ─────────────────────────────────────────

class FakeUserRepository : UserRepository {

    val usersFlow = MutableStateFlow<List<User>>(emptyList())
    var refreshResult: Result<List<User>> = Result.Success(emptyList())
    var createResult: Result<User> = Result.Error(AppException.Unknown("Not configured"))
    var deleteResult: Result<Unit> = Result.Success(Unit)

    override fun observeUsers(): Flow<List<User>> = usersFlow

    override suspend fun refreshUsers(): Result<List<User>> = refreshResult

    override suspend fun createUser(name: String, email: String, gender: String): Result<User> =
        createResult

    override suspend fun deleteUser(userId: Long): Result<Unit> = deleteResult

    override suspend fun restoreUserLocally(user: User) {
        usersFlow.value = usersFlow.value + user
    }
}
