package com.youtubie.app.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.youtubie.app.data.model.DownloadHistoryItem
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Wrapper around SharedPreferences for first-launch state, download history, and search history.
 *
 * @param context application context injected by Hilt.
 */
@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("youtubie_prefs", Context.MODE_PRIVATE)

    /**
     * Reads whether onboarding should be shown.
     *
     * @return true until onboarding has been completed.
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean("is_first_launch", true)
    }

    /**
     * Persists whether the next launch should be treated as the first launch.
     *
     * @param isFirst true to show onboarding on launch, false to skip it.
     */
    fun setFirstLaunch(isFirst: Boolean) {
        prefs.edit().putBoolean("is_first_launch", isFirst).apply()
    }

    /**
     * Reads persisted download history.
     *
     * @return saved download history, or an empty list when none exists or JSON parsing fails.
     */
    fun getHistory(): List<DownloadHistoryItem> {
        val json = prefs.getString("download_history", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<DownloadHistoryItem>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Adds a download-history entry, replacing any item with the same video ID and format.
     *
     * @param item completed download record to store.
     */
    fun addHistoryItem(item: DownloadHistoryItem) {
        val history = getHistory().toMutableList()
        // Avoid duplicate items with the same videoId and format type
        history.removeAll { it.videoId == item.videoId && it.format == item.format }
        history.add(0, item)
        val json = Gson().toJson(history)
        prefs.edit().putString("download_history", json).apply()
    }

    /**
     * Removes all persisted download-history items.
     */
    fun clearHistory() {
        prefs.edit().remove("download_history").apply()
    }

    /**
     * Reads search history, migrating legacy string-only entries into structured entries when needed.
     *
     * @return saved search history, or an empty list when none exists or JSON parsing fails.
     */
    fun getSearchHistory(): List<com.youtubie.app.data.model.SearchHistoryItem> {
        val json = prefs.getString("search_history_v2", null)
        if (json != null) {
            return try {
                val type = object : TypeToken<List<com.youtubie.app.data.model.SearchHistoryItem>>() {}.type
                Gson().fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        val oldJson = prefs.getString("search_history", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            val oldList: List<String> = Gson().fromJson(oldJson, type) ?: emptyList()
            oldList.map { com.youtubie.app.data.model.SearchHistoryItem(query = it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Adds or moves a search query to the top of the history list.
     *
     * @param query raw search URL or text.
     * @param title optional resolved video title to store with the query.
     */
    fun addSearchQuery(query: String, title: String? = null) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val list = getSearchHistory().toMutableList()
        val existingIndex = list.indexOfFirst { it.query.equals(trimmed, ignoreCase = true) }
        val existingTitle = if (existingIndex != -1) list[existingIndex].title else null
        list.removeAll { it.query.equals(trimmed, ignoreCase = true) }
        list.add(0, com.youtubie.app.data.model.SearchHistoryItem(query = trimmed, title = title ?: existingTitle))
        if (list.size > 30) list.removeAt(list.size - 1)
        val json = Gson().toJson(list)
        prefs.edit().putString("search_history_v2", json).apply()
    }

    /**
     * Updates the resolved title for a previously saved search query.
     *
     * @param query raw search URL or text used as the lookup key.
     * @param title resolved video title to store.
     */
    fun updateSearchTitle(query: String, title: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || title.isEmpty()) return
        val list = getSearchHistory().toMutableList()
        val index = list.indexOfFirst { it.query.equals(trimmed, ignoreCase = true) }
        if (index != -1) {
            list[index] = list[index].copy(title = title)
        } else {
            list.add(0, com.youtubie.app.data.model.SearchHistoryItem(query = trimmed, title = title))
        }
        if (list.size > 30) list.removeAt(list.size - 1)
        val json = Gson().toJson(list)
        prefs.edit().putString("search_history_v2", json).apply()
    }

    /**
     * Removes both current and legacy search-history keys.
     */
    fun clearSearchHistory() {
        prefs.edit().remove("search_history_v2").remove("search_history").apply()
    }
}
