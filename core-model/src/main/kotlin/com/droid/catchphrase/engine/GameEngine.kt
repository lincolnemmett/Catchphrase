package com.droid.catchphrase.engine

import com.droid.catchphrase.model.GameSettings
import com.droid.catchphrase.model.GameState
import com.droid.catchphrase.model.ScoreHistoryEntry
import com.droid.catchphrase.model.Team
import com.droid.catchphrase.model.TeamColor

class GameEngine(
    initialTeams: List<Team>,
    initialSettings: GameSettings
) {
    init {
        require(initialTeams.size in 2..4) { "Catchphrase requires 2 to 4 teams." }
        require(initialSettings.pointsToWin in 1..25) { "Points to win must be between 1 and 25." }
    }

    private var state: GameState = GameState(
        teams = initialTeams,
        scores = initialTeams.associate { it.id to 0 },
        settings = initialSettings,
        currentHoldingTeamIndex = 0,
        startingTeamIndexForRound = 0
    )

    fun getState(): GameState = state

    fun restoreState(savedState: GameState) {
        state = savedState
    }

    /**
     * Advances to the next team in rotation when "Got it" is tapped.
     */
    fun onGotIt(): Team {
        val nextIndex = (state.currentHoldingTeamIndex + 1) % state.teams.size
        state = state.copy(currentHoldingTeamIndex = nextIndex)
        return state.teams[nextIndex]
    }

    /**
     * "Skip" shows a new word without passing the phone. Turn remains with current team.
     */
    fun onSkip(): Team {
        return state.teams[state.currentHoldingTeamIndex]
    }

    /**
     * Called when the final buzzer sounds. The team holding the phone is "caught".
     *
     * Scoring Rules:
     * - 2 teams: The other team scores 1 point.
     *   (Optional bonus guess for +1 can be awarded subsequently if enabled).
     * - 3-4 teams: Every team EXCEPT the caught team scores 1 point.
     * - The caught team starts the next round.
     */
    fun onBuzzerSounded(currentWordText: String): GameState {
        val caughtTeam = state.teams[state.currentHoldingTeamIndex]
        val caughtTeamId = caughtTeam.id
        val newScores = state.scores.toMutableMap()
        val newHistory = state.scoreHistory.toMutableList()

        if (state.teams.size == 2) {
            val otherTeam = state.teams.first { it.id != caughtTeamId }
            val currentScore = newScores[otherTeam.id] ?: 0
            newScores[otherTeam.id] = currentScore + 1
            newHistory.add(
                ScoreHistoryEntry(
                    teamId = otherTeam.id,
                    pointsDelta = 1,
                    description = "Round win (${caughtTeam.name} caught)"
                )
            )
        } else {
            // 3-4 teams
            for (team in state.teams) {
                if (team.id != caughtTeamId) {
                    val currentScore = newScores[team.id] ?: 0
                    newScores[team.id] = currentScore + 1
                    newHistory.add(
                        ScoreHistoryEntry(
                            teamId = team.id,
                            pointsDelta = 1,
                            description = "Round point (${caughtTeam.name} caught)"
                        )
                    )
                }
            }
        }

        // Caught team starts the next round
        val nextRoundStartIndex = state.currentHoldingTeamIndex

        val (isOver, winnerId) = checkGameOver(newScores, state.settings.pointsToWin)

        state = state.copy(
            scores = newScores,
            caughtTeamId = caughtTeamId,
            roundWordOnBuzzer = currentWordText,
            startingTeamIndexForRound = nextRoundStartIndex,
            currentHoldingTeamIndex = nextRoundStartIndex,
            scoreHistory = newHistory,
            isGameOver = isOver,
            winnerTeamId = winnerId
        )
        return state
    }

    /**
     * In 2-team games, awards the optional bonus point (+1) to the scoring team
     * if they correctly guess the word that was on the screen.
     */
    fun awardBonusPoint(): Boolean {
        if (state.teams.size != 2 || !state.settings.bonusGuessEnabled || state.caughtTeamId == null) {
            return false
        }
        val scoringTeam = state.teams.first { it.id != state.caughtTeamId }
        val newScores = state.scores.toMutableMap()
        val current = newScores[scoringTeam.id] ?: 0
        newScores[scoringTeam.id] = current + 1

        val newHistory = state.scoreHistory.toMutableList()
        newHistory.add(
            ScoreHistoryEntry(
                teamId = scoringTeam.id,
                pointsDelta = 1,
                description = "Bonus word guess (+1)"
            )
        )

        val (isOver, winnerId) = checkGameOver(newScores, state.settings.pointsToWin)

        state = state.copy(
            scores = newScores,
            scoreHistory = newHistory,
            isGameOver = isOver,
            winnerTeamId = winnerId
        )
        return true
    }

    /**
     * Manual score adjustment (+1 or -1) on scoreboard to fix mistakes.
     */
    fun adjustScore(teamId: Int, delta: Int) {
        val current = state.scores[teamId] ?: 0
        val updated = (current + delta).coerceAtLeast(0)
        val actualDelta = updated - current
        if (actualDelta == 0) return

        val newScores = state.scores.toMutableMap()
        newScores[teamId] = updated

        val teamName = state.teams.firstOrNull { it.id == teamId }?.name ?: "Team"
        val newHistory = state.scoreHistory.toMutableList()
        newHistory.add(
            ScoreHistoryEntry(
                teamId = teamId,
                pointsDelta = actualDelta,
                description = "Manual adjustment ($teamName ${if (actualDelta > 0) "+$actualDelta" else actualDelta})"
            )
        )

        val (isOver, winnerId) = checkGameOver(newScores, state.settings.pointsToWin)

        state = state.copy(
            scores = newScores,
            scoreHistory = newHistory,
            isGameOver = isOver,
            winnerTeamId = winnerId
        )
    }

    /**
     * Undoes the last scoring action from the score history.
     */
    fun undoLastScoreAction(): Boolean {
        if (state.scoreHistory.isEmpty()) return false
        val lastAction = state.scoreHistory.last()
        val currentScore = state.scores[lastAction.teamId] ?: 0
        val revertedScore = (currentScore - lastAction.pointsDelta).coerceAtLeast(0)

        val newScores = state.scores.toMutableMap()
        newScores[lastAction.teamId] = revertedScore
        val newHistory = state.scoreHistory.dropLast(1)

        val (isOver, winnerId) = checkGameOver(newScores, state.settings.pointsToWin)

        state = state.copy(
            scores = newScores,
            scoreHistory = newHistory,
            isGameOver = isOver,
            winnerTeamId = winnerId
        )
        return true
    }

    /**
     * Prepares for the next round.
     */
    fun prepareNextRound(): GameState {
        state = state.copy(
            caughtTeamId = null,
            roundWordOnBuzzer = null,
            currentHoldingTeamIndex = state.startingTeamIndexForRound
        )
        return state
    }

    /**
     * Starts a rematch: keeps same teams and settings, resets scores and history.
     */
    fun startRematch(): GameState {
        state = state.copy(
            scores = state.teams.associate { it.id to 0 },
            currentHoldingTeamIndex = 0,
            startingTeamIndexForRound = 0,
            caughtTeamId = null,
            roundWordOnBuzzer = null,
            scoreHistory = emptyList(),
            isGameOver = false,
            winnerTeamId = null
        )
        return state
    }

    private fun checkGameOver(scores: Map<Int, Int>, pointsToWin: Int): Pair<Boolean, Int?> {
        val winners = scores.filter { it.value >= pointsToWin }
        if (winners.isEmpty()) {
            return false to null
        }
        // If multiple reach target simultaneously (possible with 3-4 teams), highest score wins
        val topScore = winners.values.maxOrNull() ?: pointsToWin
        val topTeams = winners.filter { it.value == topScore }
        // If tied for top score above threshold, sudden death continues until someone leads
        return if (topTeams.size == 1) {
            true to topTeams.keys.first()
        } else {
            false to null
        }
    }

    companion object {
        fun defaultTeams(count: Int = 2): List<Team> {
            val colors = listOf(TeamColor.CORAL, TeamColor.TEAL, TeamColor.AMBER, TeamColor.PURPLE)
            return (0 until count.coerceIn(2, 4)).map { i ->
                Team(
                    id = i + 1,
                    name = "Team ${i + 1}",
                    color = colors[i]
                )
            }
        }
    }
}
