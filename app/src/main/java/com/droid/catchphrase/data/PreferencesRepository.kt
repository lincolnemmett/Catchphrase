package com.droid.catchphrase.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.droid.catchphrase.model.GameSettings
import com.droid.catchphrase.model.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "catchphrase_prefs")

class PreferencesRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    object Keys {
        val POINTS_TO_WIN = intPreferencesKey("points_to_win")
        val BASE_ROUND_TIME = intPreferencesKey("base_round_time")
        val RANDOMIZE_ROUND_TIME = booleanPreferencesKey("randomize_round_time")
        val SHOW_COUNTDOWN = booleanPreferencesKey("show_countdown")
        val BONUS_GUESS = booleanPreferencesKey("bonus_guess")
        val TICK_HAPTICS = booleanPreferencesKey("tick_haptics")
        val SELECTED_CATEGORIES = stringSetPreferencesKey("selected_categories")

        val RECENT_WORDS = stringSetPreferencesKey("recent_words")
        val SAVED_GAME_STATE = stringPreferencesKey("saved_game_state")
    }

    val settingsFlow: Flow<GameSettings> = context.dataStore.data.map { prefs ->
        GameSettings(
            pointsToWin = prefs[Keys.POINTS_TO_WIN] ?: 7,
            baseRoundTimeSeconds = prefs[Keys.BASE_ROUND_TIME] ?: 60,
            randomizeRoundTime = prefs[Keys.RANDOMIZE_ROUND_TIME] ?: true,
            showCountdownWhenNotRandom = prefs[Keys.SHOW_COUNTDOWN] ?: false,
            bonusGuessEnabled = prefs[Keys.BONUS_GUESS] ?: true,
            tickHapticsEnabled = prefs[Keys.TICK_HAPTICS] ?: false,
            selectedCategoryIds = prefs[Keys.SELECTED_CATEGORIES] ?: emptySet()
        )
    }

    suspend fun updateSettings(settings: GameSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.POINTS_TO_WIN] = settings.pointsToWin
            prefs[Keys.BASE_ROUND_TIME] = settings.baseRoundTimeSeconds
            prefs[Keys.RANDOMIZE_ROUND_TIME] = settings.randomizeRoundTime
            prefs[Keys.SHOW_COUNTDOWN] = settings.showCountdownWhenNotRandom
            prefs[Keys.BONUS_GUESS] = settings.bonusGuessEnabled
            prefs[Keys.TICK_HAPTICS] = settings.tickHapticsEnabled
            prefs[Keys.SELECTED_CATEGORIES] = settings.selectedCategoryIds
        }
    }

    val recentWordsFlow: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECENT_WORDS] ?: emptySet()
    }

    suspend fun recordWordsSeen(words: Collection<String>) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_WORDS] ?: emptySet()
            // Keep roughly the last 1000 words
            val updated = (current + words).toList().takeLast(1000).toSet()
            prefs[Keys.RECENT_WORDS] = updated
        }
    }

    val savedGameStateFlow: Flow<GameState?> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.SAVED_GAME_STATE]
        if (!raw.isNullOrBlank()) {
            try {
                json.decodeFromString<GameState>(raw)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun saveGameState(state: GameState?) {
        context.dataStore.edit { prefs ->
            if (state == null) {
                prefs.remove(Keys.SAVED_GAME_STATE)
            } else {
                prefs[Keys.SAVED_GAME_STATE] = json.encodeToString(state)
            }
        }
    }
}
