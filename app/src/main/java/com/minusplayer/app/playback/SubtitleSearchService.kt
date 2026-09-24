package com.minusplayer.app.playback

import android.content.Context
import android.net.Uri
import com.minusplayer.app.BuildConfig
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

data class SubtitleSearchResult(
    val fileId: Long,
    val fileName: String,
    val language: String,
    val release: String?,
    val downloads: Int
)

class SubtitleSearchService(private val context: Context) {
    private val apiBase = "https://api.opensubtitles.com/api/v1"
    private val userAgent = "MinusPlayer/${BuildConfig.VERSION_NAME}"

    fun search(query: String, language: String = "en"): List<SubtitleSearchResult> {
        val apiKey = BuildConfig.OPEN_SUBTITLES_API_KEY
            .takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("OpenSubtitles API key is not configured.")

        val encodedQuery = URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
        val encodedLanguage = URLEncoder.encode(language, Charsets.UTF_8.name())
        val url = URL("$apiBase/subtitles?query=$encodedQuery&languages=$encodedLanguage&order_by=download_count&order_direction=desc&page=1")

        val json = request(url, "GET", apiKey)
        val data = json.optJSONArray("data") ?: return emptyList()

        return buildList {
            for (i in 0 until data.length()) {
                val attributes = data.optJSONObject(i)?.optJSONObject("attributes") ?: continue
                val files = attributes.optJSONArray("files") ?: continue
                val file = files.optJSONObject(0) ?: continue
                val fileId = file.optLong("file_id", -1L)
                if (fileId <= 0L) continue

                add(
                    SubtitleSearchResult(
                        fileId = fileId,
                        fileName = file.optString("file_name", "subtitle.srt"),
                        language = attributes.optString("language", language),
                        release = attributes.optString("release").takeIf { it.isNotBlank() },
                        downloads = attributes.optInt("download_count", 0)
                    )
                )
            }
        }
    }

    fun download(result: SubtitleSearchResult): Uri {
        val apiKey = BuildConfig.OPEN_SUBTITLES_API_KEY
            .takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("OpenSubtitles API key is not configured.")

        val payload = JSONObject().put("file_id", result.fileId).toString()
        val response = request(URL("$apiBase/download"), "POST", apiKey, payload)
        val link = response.optString("link").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("OpenSubtitles did not return a subtitle download link.")

        val safeName = result.fileName
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "subtitle.srt" }
        val directory = File(context.filesDir, "subtitles").apply { mkdirs() }
        val destination = File(directory, safeName)

        downloadFile(URL(link), apiKey, destination)
        return Uri.fromFile(destination)
    }

    private fun request(
        url: URL,
        method: String,
        apiKey: String,
        body: String? = null
    ): JSONObject {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Api-Key", apiKey)
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }

        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                throw IllegalStateException("OpenSubtitles request failed (${responseCode}).")
            }
            return JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadFile(url: URL, apiKey: String, destination: File) {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
            setRequestProperty("Api-Key", apiKey)
            setRequestProperty("User-Agent", userAgent)
        }

        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Subtitle download failed (${connection.responseCode}).")
            }
            connection.inputStream.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
    }
}
