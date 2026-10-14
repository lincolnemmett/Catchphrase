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
     * Behavior:
     * - Ticks start slow (~1 per second, e.g. interval ~1000ms).
     * - Accelerates continuously into a rapid trill near the end (interval ~60-80ms).
     * - All ticks are strictly before the final buzzer time.
     */
    fun generateTickOffsets(totalDurationMs: Long): List<Long> {
        val ticks = mutableListOf<Long>()
        var currentMs = 1000.0 // First tick after ~1s
        val startInterval = 1000.0
        val endInterval = 75.0

        while (currentMs < totalDurationMs - 100) {
            ticks.add(currentMs.toLong())
            val progress = (currentMs / totalDurationMs).coerceIn(0.0, 1.0)
            // Exponential acceleration: interval decreases as progress increases
            // interval(p) = start * (end / start)^p
            val interval = startInterval * Math.pow(endInterval / startInterval, progress)
            currentMs += interval
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
