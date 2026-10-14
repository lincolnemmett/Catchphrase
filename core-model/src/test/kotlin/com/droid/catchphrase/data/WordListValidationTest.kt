package com.droid.catchphrase.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WordListValidationTest {

    private val wordsDir = File("../app/src/main/assets/words")
    private val expectedCategories = listOf(
        "entertainment", "everyday", "food", "things", "nature", "phrases"
    )
    private val minPerCategory = 500
    private val minPerSubtopic = 40
    private val minRating = 4

    // Allow unicode letters, spaces, hyphens, and apostrophes
    private val allowedRegex = Regex("""^[\p{L}]+(?:[ '\-]+[\p{L}]+)*'?$""")

    private fun normalizeKey(text: String): String {
        val t = text.lowercase().replace("'", "")
        val tokens = t.split(Regex("""[\s\-]+""")).filter { it.isNotEmpty() }.toMutableList()
        if (tokens.isNotEmpty()) {
            val last = tokens.last()
            tokens[tokens.lastIndex] = when {
                last.length > 4 && last.endsWith("ies") -> last.dropLast(3) + "y"
                last.length > 3 && last.endsWith("es") && last.matches(Regex(""".*(s|x|z|ch|sh)es${'$'}""")) -> last.dropLast(2)
                last.length > 3 && last.endsWith("s") && !last.endsWith("ss") && !last.endsWith("us") -> last.dropLast(1)
                else -> last
            }
        }
        return tokens.joinToString("")
    }

    private fun wordCount(text: String): Int {
        return text.split(Regex("""[\s\-]+""")).count { it.isNotEmpty() }
    }

    @Test
    fun validateAllCategoriesAndSubtopics() {
        val globalKeys = mutableMapOf<String, String>() // normalizedKey -> categoryId

        for (categoryId in expectedCategories) {
            val file = File(wordsDir, "$categoryId.json")
            assertTrue("File for $categoryId must exist: ${file.absolutePath}", file.exists())

            val content = file.readText(Charsets.UTF_8)
            val json = Json { ignoreUnknownKeys = true }
            val data = json.decodeFromString<RawCategoryJson>(content)

            assertEquals("Category id must match file name", categoryId, data.id)
            assertTrue(
                "Category $categoryId must have at least $minPerCategory entries (found ${data.words.size})",
                data.words.size >= minPerCategory
            )

            val subtopicCounts = mutableMapOf<String, Int>()
            val localKeys = mutableMapOf<String, String>()

            for (entry in data.words) {
                val text = entry.text

                // 1. Valid rating (>= 4 and <= 5)
                assertTrue("Entry '$text' rating must be >= $minRating", entry.rating >= minRating)
                assertTrue("Entry '$text' rating must be <= 5", entry.rating <= 5)

                // 2. Subtopic must exist in category subtopics map
                assertTrue(
                    "Subtopic '${entry.subtopic}' for '$text' must be in defined subtopics",
                    data.subtopics.containsKey(entry.subtopic)
                )
                subtopicCounts[entry.subtopic] = (subtopicCounts[entry.subtopic] ?: 0) + 1

                // 3. Allowed characters
                assertTrue(
                    "Entry '$text' contains disallowed characters",
                    text.matches(allowedRegex)
                )

                // 4. 1 to 4 words length
                val wc = wordCount(text)
                assertTrue(
                    "Entry '$text' must have between 1 and 4 words (has $wc)",
                    wc in 1..4
                )

                // 5. No duplicates within category
                val normKey = normalizeKey(text)
                assertTrue(
                    "Duplicate entry '$text' (collides with '${localKeys[normKey]}') in $categoryId",
                    !localKeys.containsKey(normKey)
                )
                localKeys[normKey] = text

                // 6. No entry appears in two categories
                val otherCat = globalKeys[normKey]
                assertTrue(
                    "Entry '$text' in $categoryId also appears in category '$otherCat'",
                    otherCat == null
                )
                globalKeys[normKey] = categoryId
            }

            // 7. At least 40 entries per sub-topic
            for ((subtopicId, subtopicName) in data.subtopics) {
                val count = subtopicCounts[subtopicId] ?: 0
                assertTrue(
                    "Subtopic '$subtopicName' ($subtopicId) in $categoryId has $count entries (< $minPerSubtopic)",
                    count >= minPerSubtopic
                )
            }
        }
    }
}

@kotlinx.serialization.Serializable
private data class RawCategoryJson(
    val id: String,
    val name: String,
    val subtopics: Map<String, String>,
    val words: List<RawWordJson>
)

@kotlinx.serialization.Serializable
private data class RawWordJson(
    val text: String,
    val subtopic: String,
    val rating: Int
)
