package com.hwatch.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hwatch.app.ui.auth.AuthViewModel
import com.hwatch.app.ui.dashboard.DashboardViewModel
import com.hwatch.app.ui.downloads.DownloadsViewModel
import com.hwatch.app.ui.library.LibraryViewModel
import com.hwatch.app.ui.player.PlayerViewModel
import com.hwatch.app.ui.server.ServerSetupViewModel
import com.hwatch.app.ui.settings.SettingsViewModel
import com.hwatch.app.ui.subscriptions.AddSubscriptionViewModel
import com.hwatch.app.ui.subscriptions.SubscriptionsViewModel

/** 手动 DI 的 ViewModel 工厂，从 [AppContainer] 取依赖。 */
class HWatchViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ServerSetupViewModel::class.java) ->
                ServerSetupViewModel(
                    connectionRepository = container.serverConnectionRepository,
                    configRepository = container.serverConfigStore,
                ) as T

            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(repository = container.dashboardRepository) as T

            modelClass.isAssignableFrom(LibraryViewModel::class.java) ->
                LibraryViewModel(repository = container.libraryRepository) as T

            modelClass.isAssignableFrom(PlayerViewModel::class.java) ->
                PlayerViewModel(playerFactory = container.playerFactory) as T

            modelClass.isAssignableFrom(DownloadsViewModel::class.java) ->
                DownloadsViewModel(
                    repository = container.downloadRepository,
                    transport = container.downloadEventsTransport,
                    json = container.json,
                    baseUrlProvider = { container.currentBaseUrl() },
                ) as T

            modelClass.isAssignableFrom(SubscriptionsViewModel::class.java) ->
                SubscriptionsViewModel(repository = container.subscriptionRepository) as T

            modelClass.isAssignableFrom(AddSubscriptionViewModel::class.java) ->
                AddSubscriptionViewModel(repository = container.subscriptionRepository) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(
                    repository = container.settingsRepository,
                    configRepository = container.serverConfigStore,
                ) as T

            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(
                    authRepository = container.authRepository,
                    sessionManager = container.authSessionManager,
                    configRepository = container.serverConfigStore,
                ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
