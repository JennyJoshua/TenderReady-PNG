package com.tenderready.png.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNav(vm: MainViewModel) {
    val nav = rememberNavController()
    val go: (String) -> Unit = { route -> nav.navigate(route) { launchSingleTop = true } }
    val back: () -> Unit = { nav.popBackStack() }
    val idArg = listOf(navArgument("id") { type = NavType.LongType })

    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(vm, go) }
        composable("profile") { ProfileScreen(vm, back) }
        composable("capability/{id}", arguments = idArg) { e ->
            CapabilityScreen(vm, e.arguments?.getLong("id") ?: 0L, back)
        }
        composable("quotation/{id}", arguments = idArg) { e ->
            QuotationScreen(vm, e.arguments?.getLong("id") ?: 0L, back)
        }
        composable("tenders") { TendersListScreen(vm, go, back) }
        composable("tender/{id}", arguments = idArg) { e ->
            TenderDetailScreen(vm, e.arguments?.getLong("id") ?: 0L, back)
        }
        composable("readiness") { ReadinessScreen(vm, back) }
        composable("saved") { SavedScreen(vm, go, back) }
        composable("settings") { SettingsScreen(vm, back) }
    }
}
