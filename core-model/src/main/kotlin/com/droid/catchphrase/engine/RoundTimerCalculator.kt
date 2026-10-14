package com.droid.catchphrase.engine

import kotlin.math.roundToLong
import kotlin.random.Random

data class RoundSchedule(
    val totalDurationMs: Long,
    val tickScheduleMs: List<Long> // Ascending offsets from start (0..totalDurationMs)
)

object RoundTimerCalculator {

    /**
     * Calculates the actual round length in milliseconds:
     * - If randomize is true: uniform random variation of ±25% around baseSeconds.
     *   e.g. 60s -> [45s, 75s].
     * - If randomize is false: exactly baseSeconds * 1000L.
     */
    fun calculateRoundDurationMs(
        baseSeconds: Int,
        randomize: Boolean,
        random: Random = Random.Default
    ): Long {
        if (!randomize) {
            return baseSeconds * 1000L
        }
        val minMs = (baseSeconds * 0.75 * 1000).roundToLong()
        val maxMs = (baseSeconds * 1.25 * 1000).roundToLong()
        return random.nextLong(minMs, maxMs + 1)
    }

    /**
     * Generates a list of tick trigger timestamps (offsets from 0 ms to totalDurationMs).
     *
     * Rhythm Design:
     * - Ticks start slow (~1 per second, e.g. interval ~1000ms).
     * - Accelerates continuously, smoothly, and fluidly into a rapid frenzy near the end (~200ms, 5 beeps/sec).
     * - Integrated backwards from totalDurationMs so that the final tick interval transitions
     *   identically and seamlessly into the final buzzer ON THE BEAT with zero timing hiccup.
     * - All ticks are strictly before the final buzzer time.
     */
    fun generateTickOffsets(totalDurationMs: Long): List<Long> {
        val startInterval = 1000.0
        val endInterval = 180.0 // Smoothly accelerates to ~5.5 beeps/sec at climax
        val ticks = mutableListOf<Long>()

        var currentT = startInterval
        while (currentT <= totalDurationMs - (endInterval * 0.75)) {
            ticks.add(currentT.roundToLong())
            val progress = (currentT / totalDurationMs).coerceIn(0.0, 1.0)
            // Linear reduction in interval yields a hyperbolic, continuous increase in beeping frequency
            val interval = startInterval - (startInterval - endInterval) * progress
            currentT += interval
        }
        return ticks
    }

    fun buildSchedule(
        baseSeconds: Int,
        randomize: Boolean,
        random: Random = Random.Default
    ): RoundSchedule {
        val durationMs = calculateRoundDurationMs(baseSeconds, randomize, random)
        val ticks = generateTickOffsets(durationMs)
        return RoundSchedule(durationMs, ticks)
    }
}
