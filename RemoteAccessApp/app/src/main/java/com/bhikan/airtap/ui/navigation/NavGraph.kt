package com.bhikan.airtap.ui.navigation

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Main : Screen("main")
    object Settings : Screen("settings")
}
