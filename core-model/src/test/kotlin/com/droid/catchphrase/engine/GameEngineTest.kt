package com.droid.catchphrase.engine

import com.droid.catchphrase.model.GameSettings
import com.droid.catchphrase.model.Team
import com.droid.catchphrase.model.TeamColor
import com.droid.catchphrase.model.WordEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private val teamA = Team(1, "Red Rockets", TeamColor.CORAL)
    private val teamB = Team(2, "Blue Comets", TeamColor.TEAL)
    private val teamC = Team(3, "Green Giants", TeamColor.AMBER)

    @Test
    fun testTwoTeamsRotationAndScoring() {
        val engine = GameEngine(listOf(teamA, teamB), GameSettings(pointsToWin = 7))
        assertEquals(0, engine.getState().currentHoldingTeamIndex)

        // Tapping Got it rotates to Team B
        val holdingAfterGotIt = engine.onGotIt()
        assertEquals(teamB, holdingAfterGotIt)
        assertEquals(1, engine.getState().currentHoldingTeamIndex)

        // Tapping Skip keeps turn on Team B
        val holdingAfterSkip = engine.onSkip()
        assertEquals(teamB, holdingAfterSkip)
        assertEquals(1, engine.getState().currentHoldingTeamIndex)

        // Buzzer sounds while Team B is holding
        val roundEndState = engine.onBuzzerSounded("Pizza")
        assertEquals(teamB.id, roundEndState.caughtTeamId)
        assertEquals("Pizza", roundEndState.roundWordOnBuzzer)

        // Team A (other team) should score 1 point
        assertEquals(1, roundEndState.scores[teamA.id])
        assertEquals(0, roundEndState.scores[teamB.id])

        // The caught team (Team B) must start the next round
        assertEquals(1, roundEndState.startingTeamIndexForRound)

        // Award bonus point to Team A
        val bonusAwarded = engine.awardBonusPoint()
        assertTrue(bonusAwarded)
        assertEquals(2, engine.getState().scores[teamA.id])
    }

    @Test
    fun testThreeTeamsScoringNoBonus() {
        val engine = GameEngine(listOf(teamA, teamB, teamC), GameSettings(pointsToWin = 7))

        // Rotate to Team C
        engine.onGotIt() // Team B
        engine.onGotIt() // Team C

        // Buzzer sounds while Team C is holding
        val roundEndState = engine.onBuzzerSounded("Giraffe")
        assertEquals(teamC.id, roundEndState.caughtTeamId)

        // Every team EXCEPT caught team gets 1 point
        assertEquals(1, roundEndState.scores[teamA.id])
        assertEquals(1, roundEndState.scores[teamB.id])
        assertEquals(0, roundEndState.scores[teamC.id])

        // No bonus for 3+ teams
        val bonusAwarded = engine.awardBonusPoint()
        assertFalse(bonusAwarded)
    }

    @Test
    fun testUndoAndManualScoreAdjustments() {
        val engine = GameEngine(listOf(teamA, teamB), GameSettings(pointsToWin = 7))
        engine.onBuzzerSounded("Toothbrush") // Team A caught -> Team B gets 1
        assertEquals(1, engine.getState().scores[teamB.id])

        // Manual adjustment on Team A: +2
        engine.adjustScore(teamA.id, +2)
        assertEquals(2, engine.getState().scores[teamA.id])

        // Undo manual adjustment
        val undoSuccess = engine.undoLastScoreAction()
        assertTrue(undoSuccess)
        assertEquals(0, engine.getState().scores[teamA.id])

        // Undo round point
        engine.undoLastScoreAction()
        assertEquals(0, engine.getState().scores[teamB.id])
    }

    @Test
    fun testGameOverAndRematch() {
        val engine = GameEngine(listOf(teamA, teamB), GameSettings(pointsToWin = 2))
        engine.onGotIt() // Team B holding
        engine.onBuzzerSounded("Word 1") // Team A gets 1
        assertFalse(engine.getState().isGameOver)

        engine.prepareNextRound()
        engine.onGotIt() // Team A holding
        engine.onBuzzerSounded("Word 2") // Team B gets 1
        assertFalse(engine.getState().isGameOver)

        engine.prepareNextRound()
        engine.onGotIt() // Team B holding
        engine.onBuzzerSounded("Word 3") // Team A gets 2 -> reaches target
        assertTrue(engine.getState().isGameOver)
        assertEquals(teamA.id, engine.getState().winnerTeamId)

        // Rematch
        engine.startRematch()
        assertFalse(engine.getState().isGameOver)
        assertEquals(0, engine.getState().scores[teamA.id])
        assertEquals(0, engine.getState().scores[teamB.id])
    }

    @Test
    fun testRoundTimerScheduleCalculation() {
        // Randomize OFF: duration exactly baseSeconds
        val exactMs = RoundTimerCalculator.calculateRoundDurationMs(60, randomize = false)
        assertEquals(60_000L, exactMs)

        // Randomize ON: duration in 45..75s for 60s base
        for (i in 0 until 50) {
            val randomMs = RoundTimerCalculator.calculateRoundDurationMs(60, randomize = true, random = Random(i))
            assertTrue("Duration $randomMs should be >= 45000", randomMs >= 45_000L)
            assertTrue("Duration $randomMs should be <= 75000", randomMs <= 75_000L)
        }

        // Tick schedule: ticks must be strictly increasing and end before total duration
        val schedule = RoundTimerCalculator.buildSchedule(60, randomize = false)
        assertTrue(schedule.tickScheduleMs.isNotEmpty())
        for (i in 0 until schedule.tickScheduleMs.size - 1) {
            val t1 = schedule.tickScheduleMs[i]
            val t2 = schedule.tickScheduleMs[i + 1]
            assertTrue("Ticks must be strictly increasing ($t1 < $t2)", t1 < t2)
            // Intervals must accelerate (decrease over time)
        }
        assertTrue(schedule.tickScheduleMs.last() < schedule.totalDurationMs)

        // Interval near the beginning should be much larger than near the end
        val firstInterval = schedule.tickScheduleMs[1] - schedule.tickScheduleMs[0]
        val lastInterval = schedule.tickScheduleMs.last() - schedule.tickScheduleMs[schedule.tickScheduleMs.size - 2]
        assertTrue("First interval ($firstInterval) should be larger than last interval ($lastInterval)", firstInterval > lastInterval)
    }

    @Test
    fun testWordDealerNoRepeatsAndDeprioritization() {
        val word1 = WordEntry("Pizza", "dishes", 5)
        val word2 = WordEntry("Giraffe", "animals", 5)
        val word3 = WordEntry("Toothbrush", "household", 5)
        val word4 = WordEntry("Spider-Man", "characters", 5)

        val words = listOf(word1, word2, word3, word4)
        val recentWords = setOf("Pizza", "Giraffe")

        val dealer = WordDealer(words, recentWords)

        val dealtWords = mutableListOf<String>()
        var next = dealer.nextWord()
        while (next != null) {
            dealtWords.add(next.text)
            next = dealer.nextWord()
        }

        // All 4 words dealt
        assertEquals(4, dealtWords.size)
        // No duplicates
        assertEquals(4, dealtWords.distinct().size)

        // The non-recent words ("Toothbrush", "Spider-Man") should have been dealt first
        val firstTwo = dealtWords.take(2).toSet()
        assertTrue(firstTwo.contains("Toothbrush"))
        assertTrue(firstTwo.contains("Spider-Man"))
    }
}
