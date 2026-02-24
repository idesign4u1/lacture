package com.jewish.calendar.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jewish.calendar.ui.screens.bot.HalachicBotScreen
import com.jewish.calendar.ui.screens.calendar.CalendarScreen
import com.jewish.calendar.ui.screens.mikveh.MikvehScreen
import com.jewish.calendar.ui.screens.zmanim.ZmanimScreen

sealed class Screen(
    val route: String,
    val labelHebrew: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
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
        unselectedIcon = Icons.Outlined.Water
    )
    object Bot : Screen(
        route = "bot",
        labelHebrew = "שאל רב",
        selectedIcon = Icons.Filled.Forum,
        unselectedIcon = Icons.Outlined.Forum
    )
}

val bottomNavItems = listOf(
    Screen.Calendar,
    Screen.Zmanim,
    Screen.Mikveh,
    Screen.Bot
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
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
            composable(Screen.Calendar.route) {
                CalendarScreen()
            }
            composable(Screen.Zmanim.route) {
                ZmanimScreen()
            }
            composable(Screen.Mikveh.route) {
                MikvehScreen()
            }
            composable(Screen.Bot.route) {
                HalachicBotScreen()
            }
        }
    }
}
