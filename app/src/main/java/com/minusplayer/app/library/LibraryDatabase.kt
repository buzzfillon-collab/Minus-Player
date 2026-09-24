package com.minusplayer.app.library

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri

class LibraryDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_MEDIA (
                uri TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                mime_type TEXT NOT NULL,
                size_bytes INTEGER NOT NULL,
                duration_ms INTEGER NOT NULL,
                modified_epoch_seconds INTEGER NOT NULL,
                type INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_media_type ON $TABLE_MEDIA(type)")
        db.execSQL("CREATE INDEX idx_media_name ON $TABLE_MEDIA(name COLLATE NOCASE)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 is the initial schema. Future schema changes belong here.
    }

    fun readAll(): List<LibraryItem> {
        val result = ArrayList<LibraryItem>()
        readableDatabase.query(
            TABLE_MEDIA,
            COLUMNS,
            null,
            null,
            null,
            null,
            "name COLLATE NOCASE ASC"
        ).use { cursor ->
            val uriIndex = cursor.getColumnIndexOrThrow("uri")
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val mimeIndex = cursor.getColumnIndexOrThrow("mime_type")
            val sizeIndex = cursor.getColumnIndexOrThrow("size_bytes")
            val durationIndex = cursor.getColumnIndexOrThrow("duration_ms")
            val modifiedIndex = cursor.getColumnIndexOrThrow("modified_epoch_seconds")
            val typeIndex = cursor.getColumnIndexOrThrow("type")

            while (cursor.moveToNext()) {
                result += LibraryItem(
                    uri = Uri.parse(cursor.getString(uriIndex)),
                    name = cursor.getString(nameIndex),
                    mimeType = cursor.getString(mimeIndex),
                    sizeBytes = cursor.getLong(sizeIndex),
                    durationMs = cursor.getLong(durationIndex),
                    modifiedEpochSeconds = cursor.getLong(modifiedIndex),
                    type = if (cursor.getInt(typeIndex) == TYPE_VIDEO) {
                        LibraryType.VIDEO
                    } else {
                        LibraryType.AUDIO
                    }
                )
            }
        }
        return result
    }

    fun replaceAll(items: Collection<LibraryItem>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_MEDIA, null, null)
            val statement = db.compileStatement(
                """
                INSERT INTO $TABLE_MEDIA
                (uri, name, mime_type, size_bytes, duration_ms, modified_epoch_seconds, type)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )

            items.forEach { item ->
                statement.clearBindings()
                statement.bindString(1, item.uri.toString())
                statement.bindString(2, item.name)
                statement.bindString(3, item.mimeType)
                statement.bindLong(4, item.sizeBytes)
                statement.bindLong(5, item.durationMs)
                statement.bindLong(6, item.modifiedEpochSeconds)
                statement.bindLong(7, if (item.type == LibraryType.VIDEO) TYPE_VIDEO.toLong() else TYPE_AUDIO.toLong())
                statement.executeInsert()
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun close() {
        super.close()
    }

    companion object {
        private const val DATABASE_NAME = "minus_player_library.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_MEDIA = "media"
        private const val TYPE_VIDEO = 0
        private const val TYPE_AUDIO = 1

        private val COLUMNS = arrayOf(
            "uri",
            "name",
            "mime_type",
            "size_bytes",
            "duration_ms",
            "modified_epoch_seconds",
            "type"
        )
    }
}
