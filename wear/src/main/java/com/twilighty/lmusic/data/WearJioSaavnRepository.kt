package com.twilighty.lmusic.data

import android.net.Uri
import com.twilighty.lmusic.data.jiosaavn.JioSaavnApi
import com.twilighty.lmusic.data.jiosaavn.JioSaavnAudioQuality
import com.twilighty.lmusic.data.jiosaavn.JioSaavnClients
import com.twilighty.lmusic.data.jiosaavn.JioSaavnParsers
import com.twilighty.lmusic.data.jiosaavn.JioSaavnSession
import com.twilighty.lmusic.data.jiosaavn.JioSaavnStream
import com.twilighty.lmusic.data.jiosaavn.JioSaavnStreamFinder
import com.twilighty.lmusic.data.jiosaavn.JioSaavnTrack
import com.twilighty.lmusic.shared.WearHandoffSong
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JioSaavn straight from the watch over its own Wi-Fi or LTE, so it works with no phone.
 * Signed out, asking for 320 kbps: JioSaavn decides what it hands out, and the finder falls back to 160 kbps
 * when 320 is refused. Over the phone's Bluetooth proxy it asks for 160 kbps, which that link can keep up with.
 */
@Singleton
class WearJioSaavnRepository @Inject constructor(private val network: WearStreamingNetwork) {

    private val api = JioSaavnApi(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).build())
    private val streamFinder = JioSaavnStreamFinder(api)
    private val streams = ConcurrentHashMap<String, CachedStream>()
    private val session = JioSaavnSession()

    suspend fun search(query: String): List<JioSaavnTrack> =
        JioSaavnParsers.parseSearchSongs(api.searchSongs(session, query))

    /** A playable signed URL for the song, reused for a while (signed URLs are short-lived). */
    suspend fun resolveStream(songId: String): JioSaavnStream {
        streams[songId]?.takeIf { it.usableUntilMs > System.currentTimeMillis() }?.let { return it.stream }
        val quality = if (network.isFast) JioSaavnAudioQuality.HIGH else JioSaavnAudioQuality.NORMAL
        val stream = streamFinder.find(session, songId, quality)
        streams[songId] = CachedStream(stream, System.currentTimeMillis() + STREAM_TTL_MS)
        return stream
    }

    /** A signed URL was refused (usually expired): the next load signs a new one. */
    fun reportStreamRejected(songId: String) {
        streams.remove(songId)
        streamFinder.forget(songId)
    }

    private class CachedStream(val stream: JioSaavnStream, val usableUntilMs: Long)

    companion object {
        private const val SCHEME = "jiosaavn"
        private const val STREAM_TTL_MS = 20L * 60 * 1000

        /** The headers jiosaavn.com's own player sends; the file server answers 403 without them. */
        val MEDIA_HEADERS = mapOf(
            "User-Agent" to JioSaavnClients.WEB_USER_AGENT,
            "Referer" to "${JioSaavnClients.ORIGIN}/",
            "Origin" to JioSaavnClients.ORIGIN,
        )

        fun trackUri(songId: String): Uri = Uri.parse("$SCHEME://track/$songId")

        fun songIdFrom(uri: Uri): String? = uri.lastPathSegment?.takeIf { uri.scheme == SCHEME && it.isNotBlank() }

        fun songId(jioSaavnId: String): String = WearHandoffSong.JIOSAAVN_PREFIX + jioSaavnId
    }
}
