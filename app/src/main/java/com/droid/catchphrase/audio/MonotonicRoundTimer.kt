package com.droid.catchphrase.audio

import android.os.SystemClock
import com.droid.catchphrase.engine.RoundSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimerTickState(
    val elapsedMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false
)

/**
 * Manages the monotonic round countdown clock and triggers low-latency sound ticks.
 * Uses SystemClock.elapsedRealtime() to eliminate timing drift across frames or GC events.
 */
class MonotonicRoundTimer(
    private val scope: CoroutineScope,
    private val soundController: SoundController
) {
    private val _timerState = MutableStateFlow(TimerTickState())
    val timerState: StateFlow<TimerTickState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    private var schedule: RoundSchedule? = null
    private var hapticsEnabled: Boolean = false

    private var startRealtimeMs: Long = 0L
    private var pausedAccumulatedMs: Long = 0L
    private var pauseTimestampMs: Long = 0L
    private var nextTickIndex: Int = 0

    private var onRoundFinishedCallback: (() -> Unit)? = null

    fun start(
        roundSchedule: RoundSchedule,
        enableTickHaptics: Boolean,
        onFinished: () -> Unit
    ) {
        stop()
        schedule = roundSchedule
        hapticsEnabled = enableTickHaptics
        onRoundFinishedCallback = onFinished

        startRealtimeMs = SystemClock.elapsedRealtime()
        pausedAccumulatedMs = 0L
        nextTickIndex = 0

        _timerState.value = TimerTickState(
            elapsedMs = 0L,
            totalDurationMs = roundSchedule.totalDurationMs,
            isRunning = true,
            isPaused = false,
            isFinished = false
        )

        launchTimerLoop()
    }

    private fun launchTimerLoop() {
        val currentSchedule = schedule ?: return
        timerJob?.cancel()

        timerJob = scope.launch {
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val elapsed = (now - startRealtimeMs) + pausedAccumulatedMs
                val total = currentSchedule.totalDurationMs

                if (elapsed >= total) {
                    // Round complete: Sound buzzer & trigger callback
                    _timerState.value = _timerState.value.copy(
                        elapsedMs = total,
                        isRunning = false,
                        isFinished = true
                    )
                    soundController.playFinalBuzzer()
                    onRoundFinishedCallback?.invoke()
                    break
                }

                _timerState.value = _timerState.value.copy(elapsedMs = elapsed)

                // Check tick schedule
                while (nextTickIndex < currentSchedule.tickScheduleMs.size &&
                    elapsed >= currentSchedule.tickScheduleMs[nextTickIndex]
                ) {
                    soundController.playTick(hapticsEnabled)
                    nextTickIndex++
                }

                // Poll every ~10ms for sub-frame monotonic accuracy
                delay(10)
            }
        }
    }

    fun pause() {
        if (!_timerState.value.isRunning || _timerState.value.isPaused) return
        timerJob?.cancel()
        pauseTimestampMs = SystemClock.elapsedRealtime()
        pausedAccumulatedMs += (pauseTimestampMs - startRealtimeMs)
        _timerState.value = _timerState.value.copy(isPaused = true)
    }

    fun resume() {
        if (!_timerState.value.isRunning || !_timerState.value.isPaused) return
        startRealtimeMs = SystemClock.elapsedRealtime()
        _timerState.value = _timerState.value.copy(isPaused = false)
        launchTimerLoop()
    }

    fun stop() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = TimerTickState(isRunning = false, isPaused = false, isFinished = false)
    }
}
