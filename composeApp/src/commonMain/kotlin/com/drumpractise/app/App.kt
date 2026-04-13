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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.drumpractise.app.metronome.LocalMetronomeEngine
import com.drumpractise.app.metronome.MetronomeEngine
import com.drumpractise.app.score.ScorePlaybackController
import com.drumpractise.app.score.warmUpVerovioScoreEngine
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.drumpractise.app.metronome.MetronomeScreen
import com.drumpractise.app.navigation.AppRoutes
import com.drumpractise.app.separationpractice.SeparationPracticeScreen
import com.drumpractise.app.settings.SettingsScreen
import com.drumpractise.app.score.MusicXmlScoreScreen
import com.drumpractise.app.theme.AppTheme
import com.drumpractise.app.accentshift.AccentShiftPracticeScreen
import com.drumpractise.app.workbench.WorkbenchScreen

@Composable
fun App() {
    val metronomeEngine = remember { MetronomeEngine() }
    LaunchedEffect(metronomeEngine) {
        metronomeEngine.warmUp()
    }
    LaunchedEffect(Unit) {
        warmUpVerovioScoreEngine()
    }
    LaunchedEffect(Unit) {
        ScorePlaybackController.warmup()
    }

    AppTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val route = backStackEntry?.destination?.route
        val showBottomBar = route == AppRoutes.WORKBENCH || route == AppRoutes.SETTINGS

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
                            selected = route == AppRoutes.SETTINGS,
                            onClick = {
                                navController.navigate(AppRoutes.SETTINGS) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text("⚙") },
                            label = { Text("设置") },
                        )
                    }
                }
            },
        ) { innerPadding ->
            Surface(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                color = MaterialTheme.colorScheme.background,
            ) {
                CompositionLocalProvider(LocalMetronomeEngine provides metronomeEngine) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.WORKBENCH,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        composable(AppRoutes.WORKBENCH) {
                            WorkbenchScreen(
                                onOpenMetronome = { navController.navigate(AppRoutes.METRONOME) },
                                onOpenMusicXmlScore = { navController.navigate(AppRoutes.MUSIC_XML_SCORE) },
                                onOpenSeparationPractice = { navController.navigate(AppRoutes.SEPARATION_PRACTICE) },
                                onOpenAccentShiftPractice = { navController.navigate(AppRoutes.ACCENT_SHIFT_PRACTICE) },
                            )
                        }
                        composable(AppRoutes.SETTINGS) { SettingsScreen() }
                        composable(AppRoutes.METRONOME) {
                            MetronomeScreen(onBack = { navController.popBackStack() })
                        }
                        composable(AppRoutes.MUSIC_XML_SCORE) {
                            MusicXmlScoreScreen(onBack = { navController.popBackStack() })
                        }
                        composable(AppRoutes.SEPARATION_PRACTICE) {
                            SeparationPracticeScreen(onBack = { navController.popBackStack() })
                        }
                        composable(AppRoutes.ACCENT_SHIFT_PRACTICE) {
                            AccentShiftPracticeScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
