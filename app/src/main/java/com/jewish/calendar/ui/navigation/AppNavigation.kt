package com.jewish.calendar.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.jewish.calendar.ui.screens.profile.ProfileScreen
import com.jewish.calendar.ui.screens.zmanim.ZmanimScreen
import com.jewish.calendar.viewmodel.AuthViewModel

sealed class Screen(
    val route: String,
    val labelHebrew: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val femaleOnly: Boolean = false
) {
    object Calendar : Screen("calendar", "לוח שנה", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Zmanim   : Screen("zmanim",   "זמנים",   Icons.Filled.Schedule,      Icons.Outlined.Schedule)
    object Mikveh   : Screen("mikveh",   "טהרה",    Icons.Filled.Water,         Icons.Outlined.Water,   femaleOnly = true)
    object Bot      : Screen("bot",      "שאל רב",  Icons.Filled.Forum,         Icons.Outlined.Forum)
    object Profile  : Screen("profile",  "פרופיל",  Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

private const val ROUTE_LOGIN    = "login"
private const val ROUTE_REGISTER = "register"

val bottomNavItems = listOf(Screen.Calendar, Screen.Zmanim, Screen.Mikveh, Screen.Bot, Screen.Profile)

@Composable
fun AppNavigation() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()

    when {
        authState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        !authState.isLoggedIn -> AuthNavigation(authViewModel)
        else -> MainNavigation(
            authViewModel = authViewModel,
            isFemale = authState.currentUser?.isFemale == true
        )
    }
}

@Composable
private fun AuthNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ROUTE_LOGIN) {
        composable(ROUTE_LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(ROUTE_REGISTER) { launchSingleTop = true } }
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
private fun MainNavigation(authViewModel: AuthViewModel, isFemale: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val visibleItems = bottomNavItems.filter { !it.femaleOnly || isFemale }
    val onSignOut = { authViewModel.signOut() }

    val onNavItemClick: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Shared NavHost — used in both portrait (Scaffold) and landscape (Row) layouts
    val navHostContent: @Composable (Modifier) -> Unit = { modifier ->
        NavHost(
            navController = navController,
            startDestination = Screen.Calendar.route,
            modifier = modifier
        ) {
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.Zmanim.route)   { ZmanimScreen() }
            if (isFemale) {
                composable(Screen.Mikveh.route) { MikvehScreen() }
            }
            composable(Screen.Bot.route) {
                HalachicBotScreen(onSignOut = onSignOut)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onSignOut = onSignOut,
                    onProfileSaved = { authViewModel.refreshUser() }
                )
            }
        }
    }

    if (isLandscape) {
        // Landscape: NavigationRail on the side instead of bottom bar
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                visibleItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationRailItem(
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.labelHebrew
                            )
                        },
                        label = {
                            Text(
                                screen.labelHebrew,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = isSelected,
                        onClick = { onNavItemClick(screen.route) }
                    )
                }
            }
            navHostContent(Modifier.weight(1f).fillMaxSize())
        }
    } else {
        // Portrait: bottom NavigationBar inside Scaffold
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
                                    screen.labelHebrew,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            onClick = { onNavItemClick(screen.route) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            navHostContent(Modifier.padding(innerPadding))
        }
    }
}
