package com.droid.catchphrase.model

import kotlinx.serialization.Serializable

@Serializable
enum class TeamColor(val lightHex: Long, val darkHex: Long) {
    CORAL(0xFFD32F2F, 0xFFFF8A80),
    TEAL(0xFF00796B, 0xFF80CBC4),
    AMBER(0xFFF57C00, 0xFFFFCC80),
    PURPLE(0xFF7B1FA2, 0xFFCE93D8)
}

@Serializable
data class Team(
    val id: Int,
    val name: String,
    val color: TeamColor
)

@Serializable
data class WordEntry(
    val text: String,
    val subtopic: String,
    val rating: Int
)

@Serializable
data class CategoryData(
    val id: String,
    val name: String,
    val subtopics: Map<String, String>,
    val words: List<WordEntry>
)

@Serializable
data class GameSettings(
    val pointsToWin: Int = 7,
    val baseRoundTimeSeconds: Int = 60,
    val randomizeRoundTime: Boolean = true,
    val showCountdownWhenNotRandom: Boolean = false,
    val bonusGuessEnabled: Boolean = true,
    val tickHapticsEnabled: Boolean = false,
    val selectedCategoryIds: Set<String> = emptySet() // empty means "Everything"
)

@Serializable
data class ScoreHistoryEntry(
    val teamId: Int,
    val pointsDelta: Int,
    val description: String
)

@Serializable
data class GameState(
    val teams: List<Team>,
    val scores: Map<Int, Int>, // teamId -> score
    val settings: GameSettings,
    val currentHoldingTeamIndex: Int = 0,
    val startingTeamIndexForRound: Int = 0,
    val caughtTeamId: Int? = null,
    val roundWordOnBuzzer: String? = null,
    val scoreHistory: List<ScoreHistoryEntry> = emptyList(),
    val isGameOver: Boolean = false,
    val winnerTeamId: Int? = null
)
