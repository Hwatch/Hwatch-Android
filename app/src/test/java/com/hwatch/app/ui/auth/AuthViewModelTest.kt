package com.hwatch.app.ui.auth

import com.hwatch.app.data.auth.AuthSessionManager
import com.hwatch.app.data.auth.ServerOrigin
import com.hwatch.app.data.repository.AuthRepository
import com.hwatch.app.testutil.FakeAuthSessionStore
import com.hwatch.app.testutil.FakeHWatchApi
import com.hwatch.app.testutil.FakeServerConfigRepository
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        api: FakeHWatchApi,
        config: FakeServerConfigRepository = FakeServerConfigRepository(initialBaseUrl = "http://nas.local:8787/"),
    ): AuthViewModel {
        val manager = AuthSessionManager(FakeAuthSessionStore(), CoroutineScope(dispatcher))
        val repository = AuthRepository(
            apiProvider = { Result.success(api) },
            sessionManager = manager,
            originProvider = { ServerOrigin.fromKey(config.currentBaseUrl) },
        )
        return AuthViewModel(repository, manager, config)
    }

    @Test
    fun blankServerGoesToNoServer() = runTest(dispatcher) {
        val vm = viewModel(FakeHWatchApi(), FakeServerConfigRepository(initialBaseUrl = ""))
        advanceUntilIdle()
        assertEquals(RootUiState.NoServer, vm.uiState.value)
    }

    @Test
    fun unconfiguredServerRequiresSetup() = runTest(dispatcher) {
        val vm = viewModel(FakeHWatchApi(onAuthStatus = { com.hwatch.app.data.auth.AuthStatusDto(configured = false) }))
        advanceUntilIdle()
        assertEquals(RootUiState.SetupRequired, vm.uiState.value)
    }

    @Test
    fun configuredButUnauthenticatedRequiresLogin() = runTest(dispatcher) {
        val vm = viewModel(
            FakeHWatchApi(onAuthStatus = { com.hwatch.app.data.auth.AuthStatusDto(configured = true, authenticated = false) }),
        )
        advanceUntilIdle()
        assertTrue(vm.uiState.value is RootUiState.LoginRequired)
    }

    @Test
    fun authenticatedGoesToMain() = runTest(dispatcher) {
        val vm = viewModel(
            FakeHWatchApi(onAuthStatus = { com.hwatch.app.data.auth.AuthStatusDto(configured = true, authenticated = true) }),
        )
        advanceUntilIdle()
        assertTrue(vm.uiState.value is RootUiState.Authenticated)
    }

    @Test
    fun statusFailureGoesToUnreachable() = runTest(dispatcher) {
        val vm = viewModel(FakeHWatchApi(onAuthStatus = { throw IOException("no route") }))
        advanceUntilIdle()
        assertEquals(RootUiState.Unreachable(AuthError.UNREACHABLE), vm.uiState.value)
    }

    @Test
    fun submitLoginSuccessGoesToMain() = runTest(dispatcher) {
        val vm = viewModel(
            FakeHWatchApi(
                onAuthStatus = { com.hwatch.app.data.auth.AuthStatusDto(configured = true, authenticated = false) },
                onAuthLogin = { com.hwatch.app.data.auth.AuthSessionDto(authenticated = true, username = it.username, csrfToken = "c") },
            ),
        )
        advanceUntilIdle()
        vm.submitLogin("admin", "password1234")
        advanceUntilIdle()
        assertTrue(vm.uiState.value is RootUiState.Authenticated)
    }

    @Test
    fun submitLoginFailureSetsFormError() = runTest(dispatcher) {
        val api = FakeHWatchApi(
            onAuthStatus = { com.hwatch.app.data.auth.AuthStatusDto(configured = true, authenticated = false) },
        )
        api.onAuthLogin = {
            throw retrofit2.HttpException(
                retrofit2.Response.error<Any>(
                    401,
                    """{"error":"用户名或密码错误"}""".toResponseBody("application/json".toMediaType()),
                ),
            )
        }
        val vm = viewModel(api)
        advanceUntilIdle()
        vm.submitLogin("admin", "wrong")
        advanceUntilIdle()
        assertEquals(AuthError.INVALID_CREDENTIALS, vm.formError.value)
        assertTrue(vm.uiState.value is RootUiState.LoginRequired)
    }

    @Test
    fun logoutReturnsToLogin() = runTest(dispatcher) {
        val vm = viewModel(
            FakeHWatchApi(onAuthStatus = { com.hwatch.app.data.auth.AuthStatusDto(configured = true, authenticated = true) }),
        )
        advanceUntilIdle()
        vm.logout()
        advanceUntilIdle()
        assertTrue(vm.uiState.value is RootUiState.LoginRequired)
    }
}
