package com.jewish.calendar.ui.navigation

import android.Manifest
import android.content.res.Configuration
import android.os.Build
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.jewish.calendar.ui.screens.auth.LoginScreen
import com.jewish.calendar.ui.screens.auth.RegisterScreen
import com.jewish.calendar.ui.screens.bot.HalachicBotScreen
import com.jewish.calendar.ui.screens.calendar.CalendarScreen
import com.jewish.calendar.ui.screens.mikveh.MikvehScreen
import com.jewish.calendar.ui.screens.profile.ProfileScreen
import com.jewish.calendar.ui.screens.tools.CompassScreen
import com.jewish.calendar.ui.screens.tools.KotelScreen
import com.jewish.calendar.ui.screens.tools.ToolsScreen
import com.jewish.calendar.ui.screens.zmanim.ZmanimScreen
import com.jewish.calendar.viewmodel.AuthViewModel

// ── Routes ────────────────────────────────────────────────────────────

private const val ROUTE_LOGIN    = "login"
private const val ROUTE_REGISTER = "register"
private const val ROUTE_COMPASS  = "compass"
private const val ROUTE_KOTEL    = "kotel"

// ── Bottom-nav screens ────────────────────────────────────────────────

sealed class Screen(
    val route: String,
    val labelHebrew: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val femaleOnly: Boolean = false
) {
    object Calendar : Screen("calendar", "לוח שנה",  Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Zmanim   : Screen("zmanim",   "זמנים",    Icons.Filled.Schedule,      Icons.Outlined.Schedule)
    object Mikveh   : Screen("mikveh",   "טהרה",     Icons.Filled.Water,         Icons.Outlined.Water, femaleOnly = true)
    object Bot      : Screen("bot",      "שאל רב",   Icons.Filled.Forum,         Icons.Outlined.Forum)
    object Tools    : Screen("tools",    "כלים",     Icons.Filled.Handyman,      Icons.Outlined.Handyman)
    object Profile  : Screen("profile",  "פרופיל",  Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

val bottomNavItems = listOf(
    Screen.Calendar, Screen.Zmanim, Screen.Mikveh, Screen.Bot, Screen.Tools, Screen.Profile
)

// Routes where the bottom bar should be HIDDEN (sub-screens / full-screen tools)
private val routesWithoutBottomBar = setOf(ROUTE_COMPASS, ROUTE_KOTEL)

// ── Root ──────────────────────────────────────────────────────────────

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

// ── Auth flow ─────────────────────────────────────────────────────────

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

// ── Main flow ─────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MainNavigation(authViewModel: AuthViewModel, isFemale: Boolean) {
    val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val permissionsState = rememberMultiplePermissionsState(requiredPermissions)
    LaunchedEffect(Unit) { permissionsState.launchMultiplePermissionRequest() }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val visibleItems = bottomNavItems.filter { !it.femaleOnly || isFemale }
    val showBottomBar = currentRoute !in routesWithoutBottomBar
    val onSignOut = { authViewModel.signOut() }

    val onNavItemClick: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

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
            composable(Screen.Bot.route) { HalachicBotScreen(onSignOut = onSignOut) }

            // Tools hub
            composable(Screen.Tools.route) {
                ToolsScreen(
                    onNavigateToCompass = { navController.navigate(ROUTE_COMPASS) },
                    onNavigateToKotel   = { navController.navigate(ROUTE_KOTEL) }
                )
            }
            // Tool sub-screens (no bottom bar)
            composable(ROUTE_COMPASS) { CompassScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_KOTEL)   { KotelScreen(onBack = { navController.popBackStack() }) }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onSignOut = onSignOut,
                    onProfileSaved = { authViewModel.refreshUser() }
                )
            }
        }
    }

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (showBottomBar) {
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
            }
            navHostContent(Modifier.weight(1f).fillMaxSize())
        }
    } else {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
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
            }
        ) { innerPadding ->
            navHostContent(Modifier.padding(innerPadding))
        }
    }
}
