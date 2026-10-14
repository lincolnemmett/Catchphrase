package com.droid.catchphrase.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.droid.catchphrase.audio.MonotonicRoundTimer
import com.droid.catchphrase.audio.SoundController
import com.droid.catchphrase.data.PreferencesRepository
import com.droid.catchphrase.data.WordRepository
import com.droid.catchphrase.engine.GameEngine
import com.droid.catchphrase.engine.RoundSchedule
import com.droid.catchphrase.engine.RoundTimerCalculator
import com.droid.catchphrase.engine.WordDealer
import com.droid.catchphrase.model.GameSettings
import com.droid.catchphrase.model.GameState
import com.droid.catchphrase.model.Team
import com.droid.catchphrase.model.WordEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val preferencesRepository = PreferencesRepository(application)
    val wordRepository = WordRepository(application)
    val soundController = SoundController(application)
    val timer = MonotonicRoundTimer(viewModelScope, soundController)

    val savedSettings: StateFlow<GameSettings> = preferencesRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameSettings())

    val savedGameState: StateFlow<GameState?> = preferencesRepository.savedGameStateFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _currentWord = MutableStateFlow<WordEntry?>(null)
    val currentWord: StateFlow<WordEntry?> = _currentWord.asStateFlow()

    private val _setupTeams = MutableStateFlow(GameEngine.defaultTeams(2))
    val setupTeams: StateFlow<List<Team>> = _setupTeams.asStateFlow()

    private val _setupSettings = MutableStateFlow(GameSettings())
    val setupSettings: StateFlow<GameSettings> = _setupSettings.asStateFlow()

    private var engine: GameEngine? = null
    private var dealer: WordDealer? = null
    private var currentSchedule: RoundSchedule? = null

    init {
        viewModelScope.launch {
            savedSettings.collect { settings ->
                _setupSettings.value = settings
            }
        }
    }

    fun setTeamCount(count: Int) {
        val current = _setupTeams.value.toMutableList()
        val defaults = GameEngine.defaultTeams(count)
        if (count > current.size) {
            for (i in current.size until count) {
                current.add(defaults[i])
            }
        } else if (count < current.size) {
            while (current.size > count) {
                current.removeAt(current.lastIndex)
            }
        }
        _setupTeams.value = current
    }

    fun updateTeamName(index: Int, name: String) {
        val list = _setupTeams.value.toMutableList()
        if (index in list.indices) {
            list[index] = list[index].copy(name = name)
            _setupTeams.value = list
        }
    }

    fun updateSettings(newSettings: GameSettings) {
        _setupSettings.value = newSettings
        viewModelScope.launch {
            preferencesRepository.updateSettings(newSettings)
        }
    }

    fun startNewGame(onReady: () -> Unit) {
        viewModelScope.launch {
            val teams = _setupTeams.value
            val settings = _setupSettings.value
            val gameEngine = GameEngine(teams, settings)
            engine = gameEngine

            val words = wordRepository.getWordsForCategories(settings.selectedCategoryIds)
            dealer = WordDealer(words)

            _gameState.value = gameEngine.getState()
            preferencesRepository.saveGameState(gameEngine.getState())
            onReady()
        }
    }

    fun resumeSavedGame(saved: GameState, onReady: () -> Unit) {
        viewModelScope.launch {
            val gameEngine = GameEngine(saved.teams, saved.settings)
            gameEngine.restoreState(saved)
            engine = gameEngine

            val words = wordRepository.getWordsForCategories(saved.settings.selectedCategoryIds)
            dealer = WordDealer(words)

            _gameState.value = saved
            onReady()
        }
    }

    fun startRound(onBuzzerFinished: () -> Unit) {
        val eng = engine ?: return
        val d = dealer ?: return
        val currentSt = eng.prepareNextRound()
        _gameState.value = currentSt

        // Deal first word
        val word = d.nextWord()
        _currentWord.value = word

        // Calculate schedule
        val schedule = RoundTimerCalculator.buildSchedule(
            baseSeconds = currentSt.settings.baseRoundTimeSeconds,
            randomize = currentSt.settings.randomizeRoundTime
        )
        currentSchedule = schedule

        timer.start(
            roundSchedule = schedule,
            enableTickHaptics = currentSt.settings.tickHapticsEnabled,
            onFinished = {
                val lastWord = _currentWord.value?.text ?: "Word"
                val updated = eng.onBuzzerSounded(lastWord)
                _gameState.value = updated
                viewModelScope.launch {
                    preferencesRepository.saveGameState(updated)
                    preferencesRepository.recordWordsSeen(d.getUsedWordsInGame())
                }
                onBuzzerFinished()
            }
        )
    }

    fun onGotIt() {
        val eng = engine ?: return
        val d = dealer ?: return
        eng.onGotIt()
        _gameState.value = eng.getState()
        _currentWord.value = d.nextWord()
    }

    fun onSkip() {
        val d = dealer ?: return
        _currentWord.value = d.nextWord()
    }

    fun awardBonusPoint(): Boolean {
        val eng = engine ?: return false
        val awarded = eng.awardBonusPoint()
        if (awarded) {
            _gameState.value = eng.getState()
            viewModelScope.launch {
                preferencesRepository.saveGameState(eng.getState())
            }
        }
        return awarded
    }

    fun adjustScore(teamId: Int, delta: Int) {
        val eng = engine ?: return
        eng.adjustScore(teamId, delta)
        _gameState.value = eng.getState()
        viewModelScope.launch {
            preferencesRepository.saveGameState(eng.getState())
        }
    }

    fun undoLastScoreAction(): Boolean {
        val eng = engine ?: return false
        val undone = eng.undoLastScoreAction()
        if (undone) {
            _gameState.value = eng.getState()
            viewModelScope.launch {
                preferencesRepository.saveGameState(eng.getState())
            }
        }
        return undone
    }

    fun startRematch(onReady: () -> Unit) {
        val eng = engine ?: return
        val reset = eng.startRematch()
        _gameState.value = reset

        val words = wordRepository.getWordsForCategories(reset.settings.selectedCategoryIds)
        dealer = WordDealer(words)

        viewModelScope.launch {
            preferencesRepository.saveGameState(reset)
            onReady()
        }
    }

    fun pauseGame() {
        timer.pause()
    }

    fun resumeGame() {
        timer.resume()
    }

    fun abandonCurrentGame() {
        timer.stop()
        _gameState.value = null
        viewModelScope.launch {
            preferencesRepository.saveGameState(null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer.stop()
        soundController.release()
    }
}
