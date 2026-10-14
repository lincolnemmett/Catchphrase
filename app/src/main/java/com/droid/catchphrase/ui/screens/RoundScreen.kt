package com.droid.catchphrase.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droid.catchphrase.ui.theme.resolveColor
import com.droid.catchphrase.viewmodel.GameViewModel

@Composable
fun RoundScreen(
    viewModel: GameViewModel,
    onRoundEnd: () -> Unit,
    onExitGame: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val currentWord by viewModel.currentWord.collectAsState()
    val timerState by viewModel.timer.timerState.collectAsState()
    val isDark = isSystemInDarkTheme()

    var showExitDialog by remember { mutableStateOf(false) }
    var isMutedWarning by remember { mutableStateOf(viewModel.soundController.isMediaVolumeZero()) }

    // Intercept Back button during round with confirmation dialog
    BackHandler {
        viewModel.pauseGame()
        showExitDialog = true
    }

    LaunchedEffect(Unit) {
        if (!timerState.isRunning && !timerState.isFinished) {
            viewModel.startRound {
                onRoundEnd()
            }
        }
    }

    val state = gameState ?: return
    val currentTeam = state.teams.getOrNull(state.currentHoldingTeamIndex) ?: state.teams.first()
    val teamAccentColor = currentTeam.color.resolveColor(isDark)

    val animatedBgColor by animateColorAsState(
        targetValue = teamAccentColor.copy(alpha = 0.12f),
        animationSpec = tween(durationMillis = 300),
        label = "teamBgColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Team badge, audio warning, and pause button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team Badge (explicitly shows who holds the phone)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = teamAccentColor,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Text(
                            text = "${currentTeam.name}'s Turn",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isMutedWarning) {
                        IconButton(onClick = { isMutedWarning = viewModel.soundController.isMediaVolumeZero() }) {
                            Icon(
                                Icons.Default.VolumeOff,
                                contentDescription = "Media Volume Muted Warning",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.pauseGame() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause Round")
                    }
                }
            }

            // Word display area (Dominant center)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentWord?.subtopic?.replace("_", " ")?.uppercase() ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val wordText = currentWord?.text ?: "Ready..."
                    val dynamicFontSize = when {
                        wordText.length > 25 -> 28.sp
                        wordText.length > 18 -> 34.sp
                        wordText.length > 12 -> 42.sp
                        else -> 52.sp
                    }

                    Text(
                        text = wordText,
                        fontSize = dynamicFontSize,
                        lineHeight = dynamicFontSize * 1.15f,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics {
                            contentDescription = "Current clue word: $wordText"
                        }
                    )
                }
            }

            // Countdown indicator if enabled
            if (!state.settings.randomizeRoundTime && state.settings.showCountdownWhenNotRandom) {
                val remainingSec = ((timerState.totalDurationMs - timerState.elapsedMs) / 1000L).coerceAtLeast(0)
                Text(
                    text = "$remainingSec s",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Dominant "Got it" & Secondary "Skip" Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Primary: GOT IT (Big & Loud)
                Button(
                    onClick = { viewModel.onGotIt() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = teamAccentColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = "GOT IT!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                // Secondary: SKIP
                OutlinedButton(
                    onClick = { viewModel.onSkip() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Skip Word (Costs Time)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Manual / Auto Pause Overlay
        if (timerState.isPaused && !showExitDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(24.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Game Paused",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pass the phone to ${currentTeam.name} before resuming.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.resumeGame() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Resume", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showExitDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Quit to Menu")
                        }
                    }
                }
            }
        }

        // Confirmation Dialog for leaving
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = {
                    showExitDialog = false
                    viewModel.resumeGame()
                },
                title = { Text("Leave Game?") },
                text = { Text("Your current progress will be preserved and can be resumed from the Home screen.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitDialog = false
                            onExitGame()
                        }
                    ) {
                        Text("Exit to Menu")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            viewModel.resumeGame()
                        }
                    ) {
                        Text("Continue Playing")
                    }
                }
            )
        }
    }
}
