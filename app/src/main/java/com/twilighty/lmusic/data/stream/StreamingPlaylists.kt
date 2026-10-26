package com.twilighty.lmusic.data.stream

import com.twilighty.lmusic.data.preferences.PlaylistPreferencesRepository
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
}
