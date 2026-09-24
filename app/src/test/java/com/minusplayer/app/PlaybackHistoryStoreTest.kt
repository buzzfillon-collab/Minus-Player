package com.minusplayer.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.minusplayer.app.playback.PlaybackHistoryStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackHistoryStoreTest {
    private fun store(): PlaybackHistoryStore {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("playback_history", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        return PlaybackHistoryStore(context)
    }

    @Test
    fun recordsAndRestoresResumePosition() {
        val store = store()
        store.record("content://media/1", "Example", 30_000L, 120_000L)

        assertEquals(30_000L, store.getResumePosition("content://media/1"))
        assertEquals("Example", store.recent().single().title)
    }

    @Test
    fun completedMediaDoesNotAppearInContinueWatching() {
        val store = store()
        store.record("content://media/1", "Finished", 119_000L, 120_000L)

        assertEquals(0L, store.getResumePosition("content://media/1"))
        assertTrue(store.recent().isEmpty())
    }

    @Test
    fun keepsMostRecentItemsFirst() {
        val store = store()
        store.record("content://media/1", "One", 10_000L, 100_000L)
        store.record("content://media/2", "Two", 20_000L, 100_000L)

        assertEquals("Two", store.recent().first().title)
        assertEquals("One", store.recent().last().title)
    }
}
