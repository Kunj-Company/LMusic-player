package com.twilighty.lmusic.data.stream

import com.twilighty.lmusic.data.jiosaavn.JioSaavnCollection
import com.twilighty.lmusic.data.preferences.PlaylistPreferencesRepository
import com.twilighty.lmusic.data.ytmusic.YouTubeMusicPlaylist
import javax.inject.Inject
import javax.inject.Singleton

/** Imports streamed playlists and albums as LMusic playlists. */
@Singleton
class StreamingPlaylists @Inject constructor(
    private val playlists: PlaylistPreferencesRepository,
) {
    /**
     * Creates the LMusic playlist [id], or replaces its name and songs when it was
     * imported before, so importing again refreshes it instead of adding a copy.
     * The songs must already be in the library.
     */
    suspend fun importOrUpdate(id: String, name: String, songIds: List<Long>, source: String) {
        val ids = songIds.map(Long::toString)
        val existing = playlists.getPlaylistsOnce().find { it.id == id }
        if (existing != null) {
            playlists.updatePlaylist(existing.copy(name = name, songIds = ids, source = source))
        } else {
            playlists.createPlaylist(name = name, songIds = ids, customId = id, source = source)
        }
    }

    companion object {
        /** The playlist an imported YouTube Music playlist or album becomes; the same each time, so it refreshes. */
        fun youTubeMusicId(playlist: YouTubeMusicPlaylist): String = "ytmusic_${playlist.playlistId}"

        /** Includes the kind: a JioSaavn album and playlist can share an id. */
        fun jioSaavnId(collection: JioSaavnCollection): String =
            "jiosaavn_${collection.kind.name.lowercase()}_${collection.id}"
    }
}
