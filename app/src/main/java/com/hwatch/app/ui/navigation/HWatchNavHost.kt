package com.hwatch.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hwatch.app.ui.dashboard.DashboardScreen
import com.hwatch.app.ui.dashboard.DashboardViewModel
import com.hwatch.app.ui.downloads.DownloadsScreen
import com.hwatch.app.ui.downloads.DownloadsViewModel
import com.hwatch.app.ui.library.LibraryScreen
import com.hwatch.app.ui.library.LibraryViewModel
import com.hwatch.app.ui.player.PlayerArgsCodec
import com.hwatch.app.ui.player.PlayerDestination
import com.hwatch.app.ui.player.PlayerScreen
import com.hwatch.app.ui.player.PlayerViewModel
import com.hwatch.app.ui.settings.SettingsScreen
import com.hwatch.app.ui.settings.SettingsViewModel
import com.hwatch.app.ui.subscriptions.AddSubscriptionViewModel
import com.hwatch.app.ui.subscriptions.SubscriptionsScreen
import com.hwatch.app.ui.subscriptions.SubscriptionsViewModel
import com.hwatch.app.util.videoDisplayTitle

/** 5 个真实 tab 的 NavHost + 独立 Player destination。 */
@Composable
fun HWatchNavHost(
    navController: NavHostController,
    viewModelFactory: ViewModelProvider.Factory,
    serverBaseUrl: String,
    onChangeServer: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.DASHBOARD.route,
        modifier = modifier,
    ) {
        composable(AppDestination.DASHBOARD.route) {
            val dashboardViewModel: DashboardViewModel = viewModel(factory = viewModelFactory)
            DashboardScreen(
                viewModel = dashboardViewModel,
                baseUrl = serverBaseUrl,
                onVideoClick = { video -> navController.navigateToPlayer(video.videoId, video.translatedTitle, video.title) },
                onSeeAllLibrary = { navController.navigateToTab(AppDestination.LIBRARY.route) },
                onChangeServer = onChangeServer,
            )
        }
        composable(AppDestination.LIBRARY.route) {
            val libraryViewModel: LibraryViewModel = viewModel(factory = viewModelFactory)
            LibraryScreen(
                viewModel = libraryViewModel,
                baseUrl = serverBaseUrl,
                onVideoClick = { video -> navController.navigateToPlayer(video.videoId, video.translatedTitle, video.title) },
                onChangeServer = onChangeServer,
            )
        }
        composable(AppDestination.DOWNLOADS.route) {
            val downloadsViewModel: DownloadsViewModel = viewModel(factory = viewModelFactory)
            DownloadsScreen(
                viewModel = downloadsViewModel,
                baseUrl = serverBaseUrl,
                onPlayVideo = { video -> navController.navigateToPlayer(video.videoId, video.translatedTitle, video.title) },
                onChangeServer = onChangeServer,
            )
        }
        composable(AppDestination.SUBSCRIPTIONS.route) {
            val subscriptionsViewModel: SubscriptionsViewModel = viewModel(factory = viewModelFactory)
            val addSubscriptionViewModel: AddSubscriptionViewModel = viewModel(factory = viewModelFactory)
            SubscriptionsScreen(
                viewModel = subscriptionsViewModel,
                addViewModel = addSubscriptionViewModel,
                onChangeServer = onChangeServer,
            )
        }
        composable(AppDestination.SETTINGS.route) {
            val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
            SettingsScreen(
                viewModel = settingsViewModel,
                onChangeServer = onChangeServer,
                onLogout = onLogout,
            )
        }
        composable(
            route = PlayerDestination.ROUTE,
            arguments = listOf(navArgument(PlayerDestination.ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString(PlayerDestination.ARG)
            val args = token?.let(PlayerArgsCodec::decode)
            if (args == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                val playerViewModel: PlayerViewModel = viewModel(factory = viewModelFactory)
                PlayerScreen(
                    viewModel = playerViewModel,
                    args = args,
                    serverBaseUrl = serverBaseUrl,
                    onBack = { navController.popBackStack() },
                    onChangeServer = onChangeServer,
                )
            }
        }
    }
}

private fun NavHostController.navigateToPlayer(videoId: String, translatedTitle: String, title: String) {
    navigate(PlayerDestination.route(videoId, videoDisplayTitle(translatedTitle, title).primary))
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
