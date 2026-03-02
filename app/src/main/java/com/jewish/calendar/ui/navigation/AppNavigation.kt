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
import com.jewish.calendar.ui.screens.tools.BlessingsScreen
import com.jewish.calendar.ui.screens.tools.ChallaScreen
import com.jewish.calendar.ui.screens.tools.CompassScreen
import com.jewish.calendar.ui.screens.tools.DailyInspirationScreen
import com.jewish.calendar.ui.screens.tools.GematriaScreen
import com.jewish.calendar.ui.screens.tools.GratitudeJournalScreen
import com.jewish.calendar.ui.screens.tools.KotelScreen
import com.jewish.calendar.ui.screens.tools.OmerScreen
import com.jewish.calendar.ui.screens.tools.PsalmsScreen
import com.jewish.calendar.ui.screens.tools.ShalomBayitScreen
import com.jewish.calendar.ui.screens.tools.SiddurScreen
import com.jewish.calendar.ui.screens.tools.SpecialPrayersScreen
import com.jewish.calendar.ui.screens.tools.SpiritualTrackingScreen
import com.jewish.calendar.ui.screens.tools.TikkunHaklaliScreen
import com.jewish.calendar.ui.screens.tools.TorahScreen
import com.jewish.calendar.ui.screens.tools.MishnaScreen
import com.jewish.calendar.ui.screens.tools.ToolsScreen
import com.jewish.calendar.ui.screens.auth.OnboardingDialog
import com.jewish.calendar.ui.screens.zmanim.ZmanimScreen
import com.jewish.calendar.viewmodel.AuthViewModel
import com.jewish.calendar.viewmodel.OnboardingViewModel

// ── Routes ────────────────────────────────────────────────────────────

private const val ROUTE_LOGIN              = "login"
private const val ROUTE_REGISTER           = "register"
private const val ROUTE_COMPASS            = "compass"
private const val ROUTE_KOTEL             = "kotel"
private const val ROUTE_SIDDUR            = "siddur"
private const val ROUTE_TIKKUN            = "tikkun"
private const val ROUTE_GEMATRIA          = "gematria"
private const val ROUTE_OMER              = "omer"
private const val ROUTE_PSALMS            = "psalms"
private const val ROUTE_DAILY_INSPIRATION = "daily_inspiration"
private const val ROUTE_GRATITUDE         = "gratitude"
private const val ROUTE_SHALOM_BAYIT      = "shalom_bayit"
private const val ROUTE_SPECIAL_PRAYERS   = "special_prayers"
private const val ROUTE_BLESSINGS         = "blessings"
private const val ROUTE_CHALLA            = "challa"
private const val ROUTE_SPIRITUAL_TRACKING = "spiritual_tracking"
private const val ROUTE_TORAH              = "torah"
private const val ROUTE_MISHNA             = "mishna"

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
private val routesWithoutBottomBar = setOf(
    ROUTE_COMPASS, ROUTE_KOTEL, ROUTE_SIDDUR, ROUTE_TIKKUN, ROUTE_GEMATRIA,
    ROUTE_OMER, ROUTE_PSALMS, ROUTE_DAILY_INSPIRATION, ROUTE_GRATITUDE,
    ROUTE_SHALOM_BAYIT, ROUTE_SPECIAL_PRAYERS, ROUTE_BLESSINGS,
    ROUTE_CHALLA, ROUTE_SPIRITUAL_TRACKING, ROUTE_TORAH, ROUTE_MISHNA
)

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

    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingState by onboardingViewModel.uiState.collectAsState()

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
                    onNavigateToCompass           = { navController.navigate(ROUTE_COMPASS) },
                    onNavigateToKotel             = { navController.navigate(ROUTE_KOTEL) },
                    onNavigateToSiddur            = { navController.navigate(ROUTE_SIDDUR) },
                    onNavigateToTikkun            = { navController.navigate(ROUTE_TIKKUN) },
                    onNavigateToGematria          = { navController.navigate(ROUTE_GEMATRIA) },
                    onNavigateToOmer              = { navController.navigate(ROUTE_OMER) },
                    onNavigateToPsalms            = { navController.navigate(ROUTE_PSALMS) },
                    onNavigateToDailyInspiration  = { navController.navigate(ROUTE_DAILY_INSPIRATION) },
                    onNavigateToGratitude         = { navController.navigate(ROUTE_GRATITUDE) },
                    onNavigateToShalomBayit       = { navController.navigate(ROUTE_SHALOM_BAYIT) },
                    onNavigateToSpecialPrayers    = { navController.navigate(ROUTE_SPECIAL_PRAYERS) },
                    onNavigateToBlessings         = { navController.navigate(ROUTE_BLESSINGS) },
                    onNavigateToChalla            = { navController.navigate(ROUTE_CHALLA) },
                    onNavigateToSpiritualTracking = { navController.navigate(ROUTE_SPIRITUAL_TRACKING) },
                    onNavigateToTorah             = { navController.navigate(ROUTE_TORAH) },
                    onNavigateToMishna            = { navController.navigate(ROUTE_MISHNA) }
                )
            }
            // Tool sub-screens (no bottom bar)
            composable(ROUTE_COMPASS)             { CompassScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_KOTEL)               { KotelScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_SIDDUR)              { SiddurScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_TIKKUN)              { TikkunHaklaliScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_GEMATRIA)            { GematriaScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_OMER)                { OmerScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_PSALMS)              { PsalmsScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_DAILY_INSPIRATION)   { DailyInspirationScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_GRATITUDE)           { GratitudeJournalScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_SHALOM_BAYIT)        { ShalomBayitScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_SPECIAL_PRAYERS)     { SpecialPrayersScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_BLESSINGS)           { BlessingsScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_CHALLA)              { ChallaScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_SPIRITUAL_TRACKING)  { SpiritualTrackingScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_TORAH)               { TorahScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_MISHNA)              { MishnaScreen(onBack = { navController.popBackStack() }) }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onSignOut = onSignOut,
                    onProfileSaved = { authViewModel.refreshUser() }
                )
            }
        }
    }

    // First-launch onboarding dialog (shown on top of main content)
    OnboardingDialog(
        uiState       = onboardingState,
        onSelectGender     = onboardingViewModel::selectGender,
        onSelectPrayerStyle = onboardingViewModel::selectPrayerStyle,
        onComplete     = onboardingViewModel::complete,
        onSkip         = onboardingViewModel::skip
    )

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
