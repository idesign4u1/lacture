package com.jewish.calendar.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads bundled Sefaria JSON files from the assets/sefaria/ directory and
 * returns formatted Hebrew prayer text for offline use.
 *
 * Asset files are downloaded from github.com/Sefaria/Sefaria-Export.
 *
 * LocalAsset reference format: "filename.json" or "filename.json|Section.Key"
 * where "Section.Key" is a dot-separated path of JSON keys to navigate before
 * extracting text (e.g. "birkat_hamazon_ashkenaz.json|Birkat Hamazon").
 */
@Singleton
class LocalSefariaRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Load and parse a Sefaria asset file.
     * @param assetRef  "filename.json" or "filename.json|Section.Path"
     * @return  Formatted Hebrew prayer text, or empty string on failure.
     */
    suspend fun getTextFromAsset(assetRef: String): String = withContext(Dispatchers.IO) {
        try {
            val parts      = assetRef.split("|", limit = 2)
            val fileName   = parts[0]
            val sectionKey = parts.getOrNull(1)

            val jsonString = context.assets.open("sefaria/$fileName").bufferedReader().readText()
            val root       = JSONObject(jsonString)
            val textNode   = root.opt("text") ?: return@withContext ""

            val node = if (sectionKey != null) navigatePath(textNode, sectionKey.split("."))
                       else textNode

            extractText(node).trim()
        } catch (e: Exception) {
            ""
        }
    }

    // ── JSON navigation ────────────────────────────────────────────────

    private fun navigatePath(node: Any, path: List<String>): Any {
        if (path.isEmpty()) return node
        val obj = node as? JSONObject ?: return node
        val next = obj.opt(path[0]) ?: return node
        return navigatePath(next, path.drop(1))
    }

    // ── Text extraction ────────────────────────────────────────────────

    /**
     * Recursively extracts Hebrew text from a JSON node.
     * - JSONArray  → each element joined by blank lines
     * - JSONObject → each value extracted recursively (keys/headers skipped)
     * - String     → HTML stripped
     */
    private fun extractText(node: Any): String = when (node) {
        is String    -> stripHtml(node)
        is JSONArray -> buildString {
            for (i in 0 until node.length()) {
                val child = node.opt(i) ?: continue
                val text  = extractText(child)
                if (text.isNotBlank()) {
                    if (isNotEmpty()) append("\n\n")
                    append(text)
                }
            }
        }
        is JSONObject -> buildString {
            for (key in node.keys()) {
                val child = node.opt(key) ?: continue
                val text  = extractText(child)
                if (text.isNotBlank()) {
                    if (isNotEmpty()) append("\n\n")
                    append(text)
                }
            }
        }
        else -> ""
    }

    // ── Chapter-level navigation (for Torah / Psalms / Mishnah) ──────────

    /**
     * Returns the number of chapters in a file whose top-level `text` is a
     * List<List<String>> (Torah books, Psalms, Mishnah tractates).
     */
    suspend fun getChapterCount(assetFile: String): Int = withContext(Dispatchers.IO) {
        try {
            val json = context.assets.open("sefaria/$assetFile").bufferedReader().readText()
            val arr  = JSONObject(json).optJSONArray("text") ?: return@withContext 0
            arr.length()
        } catch (e: Exception) { 0 }
    }

    /**
     * Returns all verses (strings) for the given 0-based chapter index.
     * HTML tags are stripped before returning.
     */
    suspend fun getChapter(assetFile: String, chapterIndex: Int): List<String> =
        withContext(Dispatchers.IO) {
            try {
                val json    = context.assets.open("sefaria/$assetFile").bufferedReader().readText()
                val chapter = JSONObject(json)
                    .optJSONArray("text")
                    ?.optJSONArray(chapterIndex)
                    ?: return@withContext emptyList()

                (0 until chapter.length())
                    .mapNotNull { i -> chapter.optString(i).takeIf { it.isNotBlank() } }
                    .map { stripHtml(it) }
            } catch (e: Exception) { emptyList() }
        }

    /**
     * Returns the first verse of the given 0-based chapter (for preview/subtitle).
     */
    suspend fun getChapterOpening(assetFile: String, chapterIndex: Int): String =
        getChapter(assetFile, chapterIndex).firstOrNull()?.take(80)?.trimEnd() ?: ""

    private fun stripHtml(html: String): String =
        html.replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
}
