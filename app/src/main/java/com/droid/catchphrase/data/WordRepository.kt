package com.droid.catchphrase.data

import android.content.Context
import com.droid.catchphrase.model.CategoryData
import com.droid.catchphrase.model.WordEntry
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

class WordRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private var cachedCategories: List<CategoryData>? = null

    fun loadAllCategories(): List<CategoryData> {
        cachedCategories?.let { return it }

        val categoryFiles = listOf(
            "entertainment.json",
            "everyday.json",
            "food.json",
            "things.json",
            "nature.json",
            "phrases.json"
        )

        val list = mutableListOf<CategoryData>()
        for (fileName in categoryFiles) {
            context.assets.open("words/$fileName").use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    val data = json.decodeFromString<CategoryData>(reader.readText())
                    list.add(data)
                }
            }
        }
        cachedCategories = list
        return list
    }

    fun getWordsForCategories(selectedCategoryIds: Set<String>): List<WordEntry> {
        val all = loadAllCategories()
        val filtered = if (selectedCategoryIds.isEmpty()) {
            all // "Everything" mode
        } else {
            all.filter { it.id in selectedCategoryIds }
        }
        return filtered.flatMap { it.words }
    }
}
