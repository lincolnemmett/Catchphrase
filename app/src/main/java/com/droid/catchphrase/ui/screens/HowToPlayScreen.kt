package com.droid.catchphrase.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How to Play", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RuleCard(
                title = "1. Setup & Seating",
                description = "Players sit in a circle, alternating teams, and pass one phone around. 2 to 4 teams can play."
            )

            RuleCard(
                title = "2. Giving Clues",
                description = "The player holding the phone sees a word and gives clues to their teammates until they guess it.\n\n" +
                        "CLUE RULES:\n" +
                        "• You CANNOT say the word itself or any part of it.\n" +
                        "• No rhymes (\"sounds like...\").\n" +
                        "• No first letters or spelling clues.\n" +
                        "• Physical gestures, sound effects, and descriptive clues are encouraged!"
            )

            RuleCard(
                title = "3. Got It & Passing",
                description = "As soon as a teammate shouts the exact word, tap 'GOT IT!' and immediately hand the phone to the next player (from the other team)."
            )

            RuleCard(
                title = "4. Skipping Words",
                description = "If you are stuck, you can tap 'Skip' to get a new word. It keeps the phone on your team and only costs precious time!"
            )

            RuleCard(
                title = "5. The Beeper & The Buzzer",
                description = "The beeper ticks start slow and speed up into a rapid frenzy. When the final buzzer sounds, whoever is holding the phone is CAUGHT!\n\n" +
                        "• 2 Teams: The other team scores 1 point. Optional: they get one guess at the buzzer word for +1 bonus!\n" +
                        "• 3-4 Teams: Every team EXCEPT the caught team scores 1 point.\n" +
                        "• The caught team starts the next round."
            )

            RuleCard(
                title = "6. Winning the Game",
                description = "The first team to reach the target score (default 7 points) wins the game!"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RuleCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
        }
    }
}
