package com.twilighty.lmusic.data.ytmusic

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object YouTubeMusicParsers {
    private val json = Json { ignoreUnknownKeys = true }

    fun parsePlayer(raw: String): YouTubeMusicPlayerPayload {
        val root = json.parseToJsonElement(raw).jsonObject
        val status = root.obj("playabilityStatus")
        val details = root.obj("videoDetails")
        val streaming = root.obj("streamingData")
        val lengthSeconds = details?.str("lengthSeconds")?.toLongOrNull()
        val formats = streaming?.get("adaptiveFormats").asArray().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            if (item.str("mimeType")?.startsWith("audio/") != true) return@mapNotNull null
            YouTubeMusicAudioFormat(
                itag = item.str("itag")?.toIntOrNull() ?: item.primitive("itag")?.intOrNull ?: 0,
                mimeType = item.str("mimeType").orEmpty(),
                bitrate = item.primitive("bitrate")?.intOrNull ?: item.str("bitrate")?.toIntOrNull() ?: 0,
                sampleRate = item.str("audioSampleRate")?.toIntOrNull(),
                contentLength = item.str("contentLength")?.toLongOrNull(),
                url = item.str("url"),
                signatureCipher = item.str("signatureCipher") ?: item.str("cipher"),
                approxDurationMs = item.str("approxDurationMs")?.toLongOrNull()
                    ?: item.primitive("approxDurationMs")?.longOrNull,
            )
        }
        return YouTubeMusicPlayerPayload(
            status = status?.str("status") ?: "ERROR",
            reason = status?.str("reason"),
            videoId = details?.str("videoId"),
            title = details?.str("title"),
            author = details?.str("author"),
            durationMs = lengthSeconds?.times(1000L),
            expiresInSeconds = streaming?.str("expiresInSeconds")?.toIntOrNull()
                ?: streaming?.primitive("expiresInSeconds")?.intOrNull,
            formats = formats,
            hlsManifestUrl = streaming?.str("hlsManifestUrl")?.takeIf { it.startsWith("https://") },
        )
    }

    /** Video rows from a plain youtube.com search. */
    fun parseVideoSearch(raw: String): List<YouTubeMusicTrack> {
        val renderers = mutableListOf<JsonObject>()
        collectVideoRenderers(json.parseToJsonElement(raw), renderers)
        return renderers.mapNotNull { video ->
            val videoId = video.str("videoId")?.takeIf { YouTubeMusicFormats.isValidVideoId(it) } ?: return@mapNotNull null
            val title = video["title"]?.let(::firstRun) ?: return@mapNotNull null
            YouTubeMusicTrack(
                videoId = videoId,
                title = title,
                artist = video["ownerText"]?.let(::firstRun).orEmpty(),
                thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                durationMs = video.obj("lengthText")?.str("simpleText")?.let(::parseDuration) ?: 0L,
            )
        }.distinctBy { it.videoId }
    }

    private fun collectVideoRenderers(element: JsonElement, into: MutableList<JsonObject>) {
        when (element) {
            is JsonObject -> {
                (element["videoRenderer"] as? JsonObject)?.let { into += it; return }
                element.values.forEach { collectVideoRenderers(it, into) }
            }
            is JsonArray -> element.forEach { collectVideoRenderers(it, into) }
            else -> Unit
        }
    }

    fun parseBrowse(raw: String): YouTubeMusicBrowsePage {
        val root = json.parseToJsonElement(raw)
        val shelves = mutableListOf<YouTubeMusicShelf>()
        val looseTracks = mutableListOf<YouTubeMusicTrack>()
        val loosePlaylists = mutableListOf<YouTubeMusicPlaylist>()
        walk(root, null, shelves, looseTracks, loosePlaylists)
        // Search now returns one flat list of mixed rows: keep songs ahead of albums and playlists.
        if (looseTracks.isNotEmpty()) shelves += YouTubeMusicShelf(title = "", tracks = looseTracks)
        if (loosePlaylists.isNotEmpty()) shelves += YouTubeMusicShelf(title = "", playlists = loosePlaylists)
        return YouTubeMusicBrowsePage(
            shelves = shelves.filter { it.tracks.isNotEmpty() || it.playlists.isNotEmpty() },
            continuation = findContinuation(root),
        )
    }

    private fun walk(
        element: JsonElement,
        shelf: ShelfBuilder?,
        shelves: MutableList<YouTubeMusicShelf>,
        looseTracks: MutableList<YouTubeMusicTrack>,
        loosePlaylists: MutableList<YouTubeMusicPlaylist>,
    ) {
        when (element) {
            is JsonObject -> {
                element["musicCarouselShelfRenderer"]?.let { renderer ->
                    collectShelf(renderer, shelves, looseTracks, loosePlaylists)
                    return
                }
                element["musicShelfRenderer"]?.let { renderer ->
                    collectShelf(renderer, shelves, looseTracks, loosePlaylists)
                    return
                }
                element["musicPlaylistShelfRenderer"]?.let { renderer ->
                    collectShelf(renderer, shelves, looseTracks, loosePlaylists)
                    return
                }
                element["musicResponsiveListItemRenderer"]?.let { renderer ->
                    val item = renderer.jsonObject
                    parseTrack(item)?.let { track ->
                        if (shelf != null) shelf.tracks += track else looseTracks += track
                    } ?: parsePlaylist(item)?.let { playlist ->
                        if (shelf != null) shelf.playlists += playlist else loosePlaylists += playlist
                    }
                    return
                }
                element["musicTwoRowItemRenderer"]?.let { renderer ->
                    val item = renderer.jsonObject
                    parseTrack(item)?.let { track ->
                        if (shelf != null) shelf.tracks += track else looseTracks += track
                    } ?: parsePlaylist(item)?.let { playlist ->
                        if (shelf != null) shelf.playlists += playlist else loosePlaylists += playlist
                    }
                    return
                }
                element.values.forEach { child ->
                    walk(child, shelf, shelves, looseTracks, loosePlaylists)
                }
            }
            is JsonArray -> element.forEach { child ->
                walk(child, shelf, shelves, looseTracks, loosePlaylists)
            }
            else -> Unit
        }
    }

    private fun parseTrack(item: JsonObject): YouTubeMusicTrack? {
        val videoId = findString(item, "videoId") ?: return null
        if (!YouTubeMusicFormats.isValidVideoId(videoId)) return null
        val columns = item["flexColumns"].asArray().mapNotNull { column ->
            firstRun(column)
        }.filter { it.isNotBlank() }
        val title = columns.firstOrNull()
            ?: item["title"]?.let(::firstRun)
            ?: return null
        // "Song • Arijit Singh • Album", "Sony Music • 88M views • 3:18": keep only names.
        val detailParts = columns.drop(1).flatMap { it.split("•") }.map { it.trim() }
        val rest = detailParts.filter { it.isNotBlank() && !isMetaText(it) }
        val rawSubtitleParts = item["subtitle"]?.let { joinRuns(it) }.orEmpty()
            .split("•")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val subtitleParts = rawSubtitleParts.filterNot(::isMetaText)
        val durationMs = detailParts.firstNotNullOfOrNull { parseDuration(it) }
            ?: rawSubtitleParts.firstNotNullOfOrNull { parseDuration(it) }
            ?: 0L
        return YouTubeMusicTrack(
            videoId = videoId,
            title = title,
            artist = rest.getOrNull(0).orEmpty().ifBlank { subtitleParts.getOrNull(0).orEmpty() },
            album = rest.getOrNull(1).orEmpty().ifBlank { subtitleParts.getOrNull(1).orEmpty() },
            thumbnailUrl = largestThumbnail(item),
            durationMs = durationMs,
        )
    }

    private fun collectShelf(
        renderer: JsonElement,
        shelves: MutableList<YouTubeMusicShelf>,
        looseTracks: MutableList<YouTubeMusicTrack>,
        loosePlaylists: MutableList<YouTubeMusicPlaylist>,
    ) {
        val builder = ShelfBuilder(shelfTitle(renderer))
        val contents = renderer.jsonObject["contents"] ?: renderer.jsonObject["items"]
        contents.asArray().forEach { child ->
            walk(child, builder, shelves, looseTracks, loosePlaylists)
        }
        shelves += builder.build()
    }

    private fun parsePlaylist(item: JsonObject): YouTubeMusicPlaylist? {
        val browseId = findString(item, "browseId")
        // Artists and channels (UC…) are not playlists; their menus only hold a radio mix.
        if (browseId?.startsWith("UC") == true) return null
        val playlistId = when {
            browseId?.startsWith("VL") == true -> browseId.removePrefix("VL")
            !browseId.isNullOrBlank() -> browseId
            else -> findString(item, "playlistId")
        } ?: return null
        if (playlistId.isBlank()) return null
        val title = item["title"]?.let(::firstRun)
            ?: item["flexColumns"].asArray().firstNotNullOfOrNull { firstRun(it) }
            ?: return null
        val subtitle = item["subtitle"]?.let { joinRuns(it) }.orEmpty()
        return YouTubeMusicPlaylist(
            playlistId = playlistId,
            title = title,
            subtitle = subtitle,
            thumbnailUrl = largestThumbnail(item),
        )
    }

    private fun shelfTitle(renderer: JsonElement): String {
        val header = renderer.jsonObject["header"]
        return header?.let { firstRun(it) }.orEmpty()
    }

    private fun findContinuation(element: JsonElement): String? {
        return when (element) {
            is JsonObject -> {
                element.obj("nextContinuationData")?.str("continuation")
                    ?: element.obj("continuationCommand")?.str("token")
                    ?: element.values.firstNotNullOfOrNull { findContinuation(it) }
            }
            is JsonArray -> element.firstNotNullOfOrNull { findContinuation(it) }
            else -> null
        }
    }

    private fun findString(element: JsonElement, key: String): String? {
        return when (element) {
            is JsonObject -> element.str(key) ?: element.values.firstNotNullOfOrNull { findString(it, key) }
            is JsonArray -> element.firstNotNullOfOrNull { findString(it, key) }
            else -> null
        }
    }

    private fun largestThumbnail(element: JsonElement): String? {
        val urls = mutableListOf<String>()
        collectThumbnailUrls(element, urls)
        return urls.lastOrNull { it.startsWith("http") }
    }

    private fun collectThumbnailUrls(element: JsonElement, into: MutableList<String>) {
        when (element) {
            is JsonObject -> {
                if (element.containsKey("thumbnails") || element.containsKey("thumbnail")) {
                    element["url"]?.jsonPrimitive?.contentOrNull?.let(into::add)
                }
                element.forEach { (key, value) ->
                    if (key == "url" && element.containsKey("width")) {
                        value.jsonPrimitive.contentOrNull?.let(into::add)
                    } else {
                        collectThumbnailUrls(value, into)
                    }
                }
            }
            is JsonArray -> element.forEach { collectThumbnailUrls(it, into) }
            else -> Unit
        }
    }

    private fun firstRun(element: JsonElement): String? = joinRuns(element).takeIf { it.isNotBlank() }

    private fun joinRuns(element: JsonElement): String {
        val texts = mutableListOf<String>()
        collectRuns(element, texts)
        return texts.joinToString("").trim()
    }

    private fun collectRuns(element: JsonElement, into: MutableList<String>) {
        when (element) {
            is JsonObject -> {
                val runs = element["runs"]
                if (runs is JsonArray && element.containsKey("runs")) {
                    runs.forEach { run ->
                        (run as? JsonObject)?.str("text")?.let(into::add)
                    }
                    return
                }
                element.values.forEach { collectRuns(it, into) }
            }
            is JsonArray -> element.forEach { collectRuns(it, into) }
            else -> Unit
        }
    }

    private fun parseDuration(text: String): Long? {
        if (!text.matches(DURATION_TEXT)) return null
        val parts = text.split(":").mapNotNull { it.toLongOrNull() }
        val seconds = when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> return null
        }
        return seconds * 1000L
    }

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.primitive(key: String): JsonPrimitive? = this[key] as? JsonPrimitive

    private fun JsonElement?.asArray(): List<JsonElement> = (this as? JsonArray)?.toList().orEmpty()

    private class ShelfBuilder(val title: String) {
        val tracks = mutableListOf<YouTubeMusicTrack>()
        val playlists = mutableListOf<YouTubeMusicPlaylist>()
        fun build() = YouTubeMusicShelf(title, tracks.toList(), playlists.toList())
    }

    private val DURATION_TEXT = Regex("""\d{1,2}:\d{2}(?::\d{2})?""")
    private val COUNT_TEXT = Regex("""[\d.,]+\s*[KMB]?\s+(views|plays|songs|subscribers|monthly audience)""", RegexOption.IGNORE_CASE)
    private val TYPE_LABELS = setOf("Song", "Video", "Episode", "Single", "Album", "EP", "Podcast")

    // Upload dates on video and episode rows: "Aug 20, 2021", "2021".
    private val DATE_TEXT = Regex("""(?:[A-Z][a-z]{2} \d{1,2}, )?\d{4}""")

    private fun isMetaText(text: String): Boolean =
        text in TYPE_LABELS || text.matches(DURATION_TEXT) || text.matches(COUNT_TEXT) || text.matches(DATE_TEXT)
}
