package com.youtubie.app.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.youtubie.app.data.model.DownloadHistoryItem
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("youtubie_prefs", Context.MODE_PRIVATE)

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean("is_first_launch", true)
    }

    fun setFirstLaunch(isFirst: Boolean) {
        prefs.edit().putBoolean("is_first_launch", isFirst).apply()
    }

    fun getHistory(): List<DownloadHistoryItem> {
        val json = prefs.getString("download_history", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<DownloadHistoryItem>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addHistoryItem(item: DownloadHistoryItem) {
        val history = getHistory().toMutableList()
        // Avoid duplicate items with the same videoId and format type
        history.removeAll { it.videoId == item.videoId && it.format == item.format }
        history.add(0, item)
        val json = Gson().toJson(history)
        prefs.edit().putString("download_history", json).apply()
    }

    fun clearHistory() {
        prefs.edit().remove("download_history").apply()
    }
}
