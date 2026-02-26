package com.jewish.calendar.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jewish.calendar.ui.screens.auth.LoginScreen
import com.jewish.calendar.ui.screens.auth.RegisterScreen
import com.jewish.calendar.ui.screens.bot.HalachicBotScreen
import com.jewish.calendar.ui.screens.calendar.CalendarScreen
import com.jewish.calendar.ui.screens.mikveh.MikvehScreen
import com.jewish.calendar.ui.screens.zmanim.ZmanimScreen
import com.jewish.calendar.viewmodel.AuthViewModel

sealed class Screen(
    val route: String,
    val labelHebrew: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val femaleOnly: Boolean = false
) {
    object Calendar : Screen(
        route = "calendar",
        labelHebrew = "לוח שנה",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )
    object Zmanim : Screen(
        route = "zmanim",
        labelHebrew = "זמנים",
        selectedIcon = Icons.Filled.Schedule,
        unselectedIcon = Icons.Outlined.Schedule
    )
    object Mikveh : Screen(
        route = "mikveh",
        labelHebrew = "טהרה",
        selectedIcon = Icons.Filled.Water,
        unselectedIcon = Icons.Outlined.Water,
        femaleOnly = true
    )
    object Bot : Screen(
        route = "bot",
        labelHebrew = "שאל רב",
        selectedIcon = Icons.Filled.Forum,
        unselectedIcon = Icons.Outlined.Forum
    )
}

private const val ROUTE_LOGIN = "login"
private const val ROUTE_REGISTER = "register"

val bottomNavItems = listOf(
    Screen.Calendar,
    Screen.Zmanim,
    Screen.Mikveh,
    Screen.Bot
)

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    when {
        authState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        !authState.isLoggedIn -> {
            AuthNavigation(authViewModel)
        }
        else -> {
            MainNavigation(
                authViewModel = authViewModel,
                isFemale = authState.currentUser?.isFemale == true
            )
        }
    }
}

@Composable
private fun AuthNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_LOGIN) {
        composable(ROUTE_LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(ROUTE_REGISTER) { launchSingleTop = true }
                }
            )
        }
        composable(ROUTE_REGISTER) {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun MainNavigation(
    authViewModel: AuthViewModel,
    isFemale: Boolean
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Tabs visible to everyone; Tahara only for women
    val visibleItems = bottomNavItems.filter { !it.femaleOnly || isFemale }

    Scaffold(
        bottomBar = {
            NavigationBar {
                visibleItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.labelHebrew
                            )
                        },
                        label = {
                            Text(
                                text = screen.labelHebrew,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Calendar.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.Zmanim.route) { ZmanimScreen() }
            if (isFemale) {
                composable(Screen.Mikveh.route) { MikvehScreen() }
            }
            composable(Screen.Bot.route) {
                HalachicBotScreen(
                    onSignOut = { authViewModel.signOut() }
                )
            }
        }
    }
}
