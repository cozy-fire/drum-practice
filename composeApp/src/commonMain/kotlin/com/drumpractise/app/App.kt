package com.drumpractise.app

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.drumpractise.app.metronome.MetronomeScreen
import com.drumpractise.app.navigation.AppRoutes
import com.drumpractise.app.score.MusicXmlScoreScreen
import com.drumpractise.app.theme.AppTheme
import com.drumpractise.app.workbench.MorePlaceholderScreen
import com.drumpractise.app.workbench.WorkbenchScreen

@Composable
fun App() {
    AppTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val route = backStackEntry?.destination?.route
        val showBottomBar = route == AppRoutes.WORKBENCH || route == AppRoutes.MORE

        Scaffold(
            contentWindowInsets =
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(
                            selected = route == AppRoutes.WORKBENCH,
                            onClick = {
                                navController.navigate(AppRoutes.WORKBENCH) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text("◇") },
                            label = { Text("工作台") },
                        )
                        NavigationBarItem(
                            selected = route == AppRoutes.MORE,
                            onClick = {
                                navController.navigate(AppRoutes.MORE) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text("⋯") },
                            label = { Text("更多") },
                        )
                    }
                }
            },
        ) { innerPadding ->
            Surface(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                color = MaterialTheme.colorScheme.background,
            ) {
                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.WORKBENCH,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(AppRoutes.WORKBENCH) {
                        WorkbenchScreen(
                            onOpenMetronome = { navController.navigate(AppRoutes.METRONOME) },
                            onOpenMusicXmlScore = { navController.navigate(AppRoutes.MUSIC_XML_SCORE) },
                        )
                    }
                    composable(AppRoutes.MORE) {
                        MorePlaceholderScreen()
                    }
                    composable(AppRoutes.METRONOME) {
                        MetronomeScreen(onBack = { navController.popBackStack() })
                    }
                    composable(AppRoutes.MUSIC_XML_SCORE) {
                        MusicXmlScoreScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
