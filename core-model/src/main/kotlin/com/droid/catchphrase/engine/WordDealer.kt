package com.droid.catchphrase.engine

import com.droid.catchphrase.model.WordEntry

class WordDealer(
    private val allAvailableWords: List<WordEntry>,
    recentWordTexts: Collection<String> = emptySet(),
    private val rng: java.util.Random = java.util.Random()
) {
    private val recentTextsSet = recentWordTexts.toSet()
    private val usedInCurrentGame = mutableSetOf<String>()

    private val primaryPool: MutableList<WordEntry>
    private val deprioritizedPool: MutableList<WordEntry>

    init {
        val nonRecent = mutableListOf<WordEntry>()
        val recent = mutableListOf<WordEntry>()

        for (w in allAvailableWords) {
            if (w.text in recentTextsSet) {
                recent.add(w)
            } else {
                nonRecent.add(w)
            }
        }

        nonRecent.shuffle(rng)
        recent.shuffle(rng)

        primaryPool = nonRecent
        deprioritizedPool = recent
    }

    /**
     * Deals the next word. Ensures:
     * 1. No word repeats within the current game.
     * 2. Deprioritizes words seen recently (in the last ~1000 history).
     * 3. Returns null if all words in all pools are exhausted.
     */
    fun nextWord(): WordEntry? {
        while (primaryPool.isNotEmpty()) {
            val candidate = primaryPool.removeAt(primaryPool.lastIndex)
            if (candidate.text !in usedInCurrentGame) {
                usedInCurrentGame.add(candidate.text)
                return candidate
            }
        }

        while (deprioritizedPool.isNotEmpty()) {
            val candidate = deprioritizedPool.removeAt(deprioritizedPool.lastIndex)
            if (candidate.text !in usedInCurrentGame) {
                usedInCurrentGame.add(candidate.text)
                return candidate
            }
        }

        return null
    }

    fun getUsedWordsInGame(): Set<String> = usedInCurrentGame.toSet()
}
