package com.dmgproductions.amp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dmgproductions.amp.player.ActivitiesScreen
import com.dmgproductions.amp.player.ActivitiesViewModel
import com.dmgproductions.amp.player.NowPlayingScreen
import com.dmgproductions.amp.player.PlayerViewModel
import com.dmgproductions.amp.ui.screens.AboutScreen
import com.dmgproductions.amp.ui.screens.HelpScreen
import com.dmgproductions.amp.ui.screens.SettingsScreen

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    PLAYER("player", "Player", Icons.Rounded.GraphicEq),
    ACTIVITIES("activities", "Activities", Icons.Rounded.DirectionsRun),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings),
}

private const val ROUTE_HELP = "help"
private const val ROUTE_ABOUT = "about"

@Composable
fun AmpApp(
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = viewModel()
    val activitiesViewModel: ActivitiesViewModel = viewModel()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = Dest.entries.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Dest.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Dest.PLAYER.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Dest.PLAYER.route) {
                val state by playerViewModel.state.collectAsStateWithLifecycle()
                NowPlayingScreen(
                    state = state,
                    onTogglePlay = playerViewModel::togglePlay,
                    onNext = playerViewModel::next,
                    onPrevious = playerViewModel::previous,
                    onSeek = playerViewModel::seekTo,
                    onSelectActivity = { playerViewModel.selectActivity(it) },
                    onToggleAutoSync = playerViewModel::setAutoSync,
                )
            }
            composable(Dest.ACTIVITIES.route) {
                val state by activitiesViewModel.state.collectAsStateWithLifecycle()
                ActivitiesScreen(
                    state = state,
                    onTrain = activitiesViewModel::train,
                    onCancel = activitiesViewModel::cancel,
                    onClearAll = activitiesViewModel::clearAll,
                )
            }
            composable(Dest.SETTINGS.route) {
                SettingsScreen(
                    dynamicColor = dynamicColor,
                    onDynamicColorChange = onDynamicColorChange,
                    onOpenHelp = { navController.navigate(ROUTE_HELP) },
                    onOpenAbout = { navController.navigate(ROUTE_ABOUT) },
                )
            }
            composable(ROUTE_HELP) { HelpScreen(onBack = { navController.popBackStack() }) }
            composable(ROUTE_ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
