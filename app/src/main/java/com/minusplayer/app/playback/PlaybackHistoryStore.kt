package com.minusplayer.app.playback

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class PlaybackHistoryItem(
    val mediaId: String,
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedEpochMs: Long
) {
    val uri: Uri get() = Uri.parse(mediaId)
}

class PlaybackHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getResumePosition(mediaId: String): Long {
        return readAll().firstOrNull { it.mediaId == mediaId }?.let { item ->
            if (item.durationMs > 0L && item.positionMs >= (item.durationMs - COMPLETION_THRESHOLD_MS).coerceAtLeast(0L)) {
                0L
            } else {
                item.positionMs.coerceAtLeast(0L)
            }
        } ?: 0L
    }

    @Synchronized
    fun record(mediaId: String, title: String, positionMs: Long, durationMs: Long) {
        if (mediaId.isBlank()) return
        val current = readAll().filterNot { it.mediaId == mediaId }.toMutableList()
        val position = positionMs.coerceAtLeast(0L)
        val completed = durationMs > 0L &&
            position >= (durationMs - COMPLETION_THRESHOLD_MS).coerceAtLeast(0L)

        if (!completed && position > MIN_RECORDED_POSITION_MS) {
            current += PlaybackHistoryItem(
                mediaId = mediaId,
                title = title.ifBlank { Uri.parse(mediaId).lastPathSegment ?: mediaId },
                positionMs = position,
                durationMs = durationMs.coerceAtLeast(0L),
                lastPlayedEpochMs = System.currentTimeMillis()
            )
        }

        val trimmed = current
            .sortedByDescending { it.lastPlayedEpochMs }
            .take(MAX_ITEMS)
        writeAll(trimmed)
    }

    @Synchronized
    fun recent(limit: Int = MAX_ITEMS): List<PlaybackHistoryItem> =
        readAll().sortedByDescending { it.lastPlayedEpochMs }.take(limit.coerceAtLeast(0))

    @Synchronized
    fun remove(mediaId: String) {
        writeAll(readAll().filterNot { it.mediaId == mediaId })
    }

    private fun readAll(): List<PlaybackHistoryItem> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        PlaybackHistoryItem(
                            mediaId = item.getString("mediaId"),
                            title = item.optString("title"),
                            positionMs = item.optLong("positionMs"),
                            durationMs = item.optLong("durationMs"),
                            lastPlayedEpochMs = item.optLong("lastPlayedEpochMs")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeAll(items: List<PlaybackHistoryItem>) {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("mediaId", it.mediaId)
                    .put("title", it.title)
                    .put("positionMs", it.positionMs)
                    .put("durationMs", it.durationMs)
                    .put("lastPlayedEpochMs", it.lastPlayedEpochMs)
            )
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "playback_history"
        private const val KEY_ITEMS = "items"
        private const val MAX_ITEMS = 50
        private const val MIN_RECORDED_POSITION_MS = 5_000L
        private const val COMPLETION_THRESHOLD_MS = 30_000L
    }
}
