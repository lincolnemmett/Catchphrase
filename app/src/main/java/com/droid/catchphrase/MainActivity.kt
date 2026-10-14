package com.droid.catchphrase

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.droid.catchphrase.ui.screens.GameSetupScreen
import com.droid.catchphrase.ui.screens.HomeScreen
import com.droid.catchphrase.ui.screens.HowToPlayScreen
import com.droid.catchphrase.ui.screens.RoundResultScreen
import com.droid.catchphrase.ui.screens.RoundScreen
import com.droid.catchphrase.ui.screens.ScoreboardScreen
import com.droid.catchphrase.ui.screens.SettingsScreen
import com.droid.catchphrase.ui.screens.WinnerScreen
import com.droid.catchphrase.ui.theme.CatchphraseTheme
import com.droid.catchphrase.viewmodel.GameViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Setup : Screen("setup")
    object Round : Screen("round")
    object RoundResult : Screen("round_result")
    object Scoreboard : Screen("scoreboard")
    object Winner : Screen("winner")
    object Settings : Screen("settings")
    object HowToPlay : Screen("how_to_play")
}

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on during rounds and party gameplay
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            CatchphraseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CatchphraseApp(viewModel)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Auto-pause when app is backgrounded or loses audio focus
        viewModel.pauseGame()
    }
}

@Composable
fun CatchphraseApp(viewModel: GameViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onStartNewGame = { navController.navigate(Screen.Setup.route) },
                onResumeGame = { navController.navigate(Screen.Scoreboard.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenHowToPlay = { navController.navigate(Screen.HowToPlay.route) }
            )
        }

        composable(Screen.Setup.route) {
            GameSetupScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onStartGame = {
                    navController.navigate(Screen.Round.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.Round.route) {
            RoundScreen(
                viewModel = viewModel,
                onRoundEnd = {
                    navController.navigate(Screen.RoundResult.route) {
                        popUpTo(Screen.Round.route) { inclusive = true }
                    }
                },
                onExitGame = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RoundResult.route) {
            RoundResultScreen(
                viewModel = viewModel,
                onContinueToScoreboard = {
                    navController.navigate(Screen.Scoreboard.route) {
                        popUpTo(Screen.RoundResult.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Scoreboard.route) {
            ScoreboardScreen(
                viewModel = viewModel,
                onStartNextRound = {
                    navController.navigate(Screen.Round.route)
                },
                onGameOver = {
                    navController.navigate(Screen.Winner.route) {
                        popUpTo(Screen.Scoreboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Winner.route) {
            WinnerScreen(
                viewModel = viewModel,
                onRematch = {
                    navController.navigate(Screen.Scoreboard.route) {
                        popUpTo(Screen.Winner.route) { inclusive = true }
                    }
                },
                onNewGame = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.HowToPlay.route) {
            HowToPlayScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
