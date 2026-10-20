package com.twilighty.lmusic.data.ytmusic

import com.twilighty.lmusic.data.database.MusicDao
import com.twilighty.lmusic.data.database.SongArtistCrossRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps YouTube Music tracks that are actually played in the songs table, so likes,
 * playlists, stats and the source filter treat them like any other cloud song.
 * Queued tracks are only remembered; each one is saved when its stream is first
 * prepared, so browsing a shelf does not flood the library.
 */
@Singleton
class YouTubeMusicLibrary @Inject constructor(
    private val musicDao: MusicDao,
) {
    private val queued = ConcurrentHashMap<String, YouTubeMusicTrack>()

    fun remember(tracks: List<YouTubeMusicTrack>) {
        tracks.forEach { queued[it.videoId] = it }
    }

    /** Saves a remembered track the first time it is about to play. */
    suspend fun saveIfQueued(videoId: String) {
        val track = queued.remove(videoId) ?: return
        runCatching { save(track) }.onFailure { Timber.w(it, "Could not save YouTube track $videoId") }
    }

    private suspend fun save(track: YouTubeMusicTrack) = withContext(Dispatchers.IO) {
        val song = YouTubeMusicUris.toEntity(track)
        musicDao.insertArtistsIgnoreConflicts(listOf(YouTubeMusicUris.toArtist(track)))
        musicDao.insertAlbumsIgnoreConflicts(listOf(YouTubeMusicUris.toAlbum(track)))
        val inserted = musicDao.insertSongsIgnoreConflicts(listOf(song))
        // An existing row only gets a fresh title, artist and cover, so likes,
        // cached lyrics and the date added stay as they were.
        if (inserted.single() == -1L) {
            musicDao.updateStreamedSongMetadata(
                id = song.id,
                title = song.title,
                artistName = song.artistName,
                artistId = song.artistId,
                albumName = song.albumName,
                albumId = song.albumId,
                albumArtUri = song.albumArtUriString,
                duration = song.duration,
            )
        }
        musicDao.deleteCrossRefsBySongIds(listOf(song.id))
        musicDao.insertSongArtistCrossRefs(
            listOf(SongArtistCrossRef(songId = song.id, artistId = song.artistId, isPrimary = true))
        )
    }
}
