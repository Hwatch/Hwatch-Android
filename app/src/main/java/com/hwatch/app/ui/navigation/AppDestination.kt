package com.hwatch.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.hwatch.app.R

enum class AppDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD("dashboard", R.string.tab_home, Icons.Filled.Home),
    LIBRARY("library", R.string.tab_library, Icons.Filled.VideoLibrary),
    DOWNLOADS("downloads", R.string.tab_downloads, Icons.Filled.Download),
    SUBSCRIPTIONS("subscriptions", R.string.tab_subscriptions, Icons.Filled.Subscriptions),
    SETTINGS("settings", R.string.tab_settings, Icons.Filled.Settings),
}