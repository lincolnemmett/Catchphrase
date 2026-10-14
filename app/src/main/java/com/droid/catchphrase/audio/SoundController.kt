package com.droid.catchphrase.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

/**
 * Low-latency audio and haptics controller for the Catchphrase beeper.
 *
 * Requirements met:
 * - Low-latency playback via SoundPool with USAGE_GAME / CONTENT_TYPE_SONIFICATION attributes,
 *   so system volume keys directly control the game volume.
 * - Detects media volume == 0 to allow showing a gentle warning.
 * - Audio tones are procedurally generated in pure code (no third party copyright dependencies):
 *     1) Tick tone: 880 Hz crisp woodblock-like blip (35ms duration with exponential envelope).
 *     2) Final buzzer: 150 Hz harsh square/sawtooth alarm buzz (900ms duration).
 * - Haptic feedback: optional tick haptics and a strong final buzzer haptic burst.
 */
class SoundController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var soundPool: SoundPool? = null
    private var tickSoundId: Int = 0
    private var buzzerSoundId: Int = 0
    private var isLoaded: Boolean = false

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val sp = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes)
            .build()

        soundPool = sp

        // Synthesize PCM WAV files in cache directory and load into SoundPool
        val cacheDir = context.cacheDir
        val tickFile = File(cacheDir, "tick_tone.wav")
        val buzzerFile = File(cacheDir, "buzzer_tone.wav")

        generateTickWav(tickFile)
        generateBuzzerWav(buzzerFile)

        tickSoundId = sp.load(tickFile.absolutePath, 1)
        buzzerSoundId = sp.load(buzzerFile.absolutePath, 1)

        sp.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                isLoaded = true
            }
        }
    }

    fun isMediaVolumeZero(): Boolean {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return current == 0
    }

    fun playTick(hapticsEnabled: Boolean) {
        soundPool?.play(tickSoundId, 1f, 1f, 1, 0, 1f)
        if (hapticsEnabled && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        }
    }

    fun playFinalBuzzer() {
        soundPool?.play(buzzerSoundId, 1f, 1f, 2, 0, 1f)
        // Strong haptic burst on buzzer
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 200, 100, 400)
                val amplitudes = intArrayOf(0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(600)
            }
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }

    companion object {
        /**
         * Generates a 35ms, 880Hz sine beep with fast exponential decay (percussive tick).
         */
        private fun generateTickWav(file: File) {
            val sampleRate = 44100
            val durationSec = 0.035
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            val freq = 880.0

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = Math.exp(-t * 90.0) // sharp percussive decay
                val wave = sin(2.0 * Math.PI * freq * t)
                samples[i] = (wave * envelope * Short.MAX_VALUE * 0.9).toInt().toShort()
            }
            writeWavFile(file, samples, sampleRate)
        }

        /**
         * Generates a 900ms, 150Hz harsh buzzer wave (fundamental + odd harmonics).
         */
        private fun generateBuzzerWav(file: File) {
            val sampleRate = 44100
            val durationSec = 0.90
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            val freq = 150.0

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // Harsh buzzer: fundamental + 3rd harmonic + 5th harmonic
                val wave = sin(2.0 * Math.PI * freq * t) +
                        0.5 * sin(2.0 * Math.PI * 3.0 * freq * t) +
                        0.25 * sin(2.0 * Math.PI * 5.0 * freq * t)
                val normalized = (wave / 1.75).coerceIn(-1.0, 1.0)
                samples[i] = (normalized * Short.MAX_VALUE * 0.95).toInt().toShort()
            }
            writeWavFile(file, samples, sampleRate)
        }

        private fun writeWavFile(file: File, samples: ShortArray, sampleRate: Int) {
            val byteRate = sampleRate * 2 // 16-bit mono = 2 bytes per sample
            val dataSize = samples.size * 2
            val totalSize = 36 + dataSize

            val header = ByteBuffer.allocate(44).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                put("RIFF".toByteArray())
                putInt(totalSize)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16) // Subchunk1Size for PCM
                putShort(1) // AudioFormat: 1 = PCM
                putShort(1) // NumChannels: 1 = Mono
                putInt(sampleRate)
                putInt(byteRate)
                putShort(2) // BlockAlign
                putShort(16) // BitsPerSample
                put("data".toByteArray())
                putInt(dataSize)
            }

            FileOutputStream(file).use { out ->
                out.write(header.array())
                val pcmBuffer = ByteBuffer.allocate(dataSize).apply {
                    order(ByteOrder.LITTLE_ENDIAN)
                    for (s in samples) {
                        putShort(s)
                    }
                }
                out.write(pcmBuffer.array())
            }
        }
    }
}
