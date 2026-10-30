package com.twilighty.lmusic.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearMusicDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WearMusicDatabase::class.java
    )

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val songs = listOf(
        LocalSongEntity(
            songId = "watch-song-1",
            title = "Amber Skies",
            artist = "Aurora Quartet",
            album = "Dawn Sessions",
            albumId = 4_294_967_301L,
            duration = 213_456L,
            mimeType = "audio/mpeg",
            fileSize = 8_765_432L,
            bitrate = 320_000,
            sampleRate = 44_100,
            isFavorite = true,
            favoriteSyncPending = true,
            paletteSeedArgb = -13_281_923,
            themePaletteJson = """{"primary":-13281923,"dark":true}""",
            artworkPath = "/data/user/0/com.twilighty.lmusic/files/artwork/amber.jpg",
            localPath = "/data/user/0/com.twilighty.lmusic/files/music/amber.mp3",
            transferredAt = 1_780_000_000_123L
        ),
        LocalSongEntity(
            songId = "watch-song-2",
            title = "Blue Horizon",
            artist = "Evening Trio",
            album = "Night Sessions",
            albumId = 4_294_967_302L,
            duration = 345_678L,
            mimeType = "audio/flac",
            fileSize = 27_654_321L,
            bitrate = 900_000,
            sampleRate = 48_000,
            isFavorite = false,
            favoriteSyncPending = false,
            paletteSeedArgb = -10_188_076,
            themePaletteJson = """{"primary":-10188076,"dark":false}""",
            artworkPath = "/data/user/0/com.twilighty.lmusic/files/artwork/blue.png",
            localPath = "/data/user/0/com.twilighty.lmusic/files/music/blue.flac",
            transferredAt = 1_780_000_000_987L
        )
    )

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migration5To6PreservesEverySongColumnAndAddsPlaylistTables() {
        createVersion5Database()

        helper.runMigrationsAndValidate(
            TEST_DB,
            6,
            true,
            *WearMusicDatabase.ALL_MIGRATIONS
        ).use { db ->
            db.query("SELECT * FROM local_songs ORDER BY songId").use { cursor ->
                assertEquals(songs.size, cursor.count)
                for (song in songs) {
                    assertTrue(cursor.moveToNext())
                    assertEquals(song.songId, cursor.getString(cursor.getColumnIndexOrThrow("songId")))
                    assertEquals(song.title, cursor.getString(cursor.getColumnIndexOrThrow("title")))
                    assertEquals(song.artist, cursor.getString(cursor.getColumnIndexOrThrow("artist")))
                    assertEquals(song.album, cursor.getString(cursor.getColumnIndexOrThrow("album")))
                    assertEquals(song.albumId, cursor.getLong(cursor.getColumnIndexOrThrow("albumId")))
                    assertEquals(song.duration, cursor.getLong(cursor.getColumnIndexOrThrow("duration")))
                    assertEquals(song.mimeType, cursor.getString(cursor.getColumnIndexOrThrow("mimeType")))
                    assertEquals(song.fileSize, cursor.getLong(cursor.getColumnIndexOrThrow("fileSize")))
                    assertEquals(song.bitrate, cursor.getInt(cursor.getColumnIndexOrThrow("bitrate")))
                    assertEquals(song.sampleRate, cursor.getInt(cursor.getColumnIndexOrThrow("sampleRate")))
                    assertEquals(
                        if (song.isFavorite) 1 else 0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("isFavorite"))
                    )
                    assertEquals(
                        if (song.favoriteSyncPending) 1 else 0,
                        cursor.getInt(cursor.getColumnIndexOrThrow("favoriteSyncPending"))
                    )
                    assertEquals(
                        song.paletteSeedArgb,
                        cursor.getInt(cursor.getColumnIndexOrThrow("paletteSeedArgb"))
                    )
                    assertEquals(
                        song.themePaletteJson,
                        cursor.getString(cursor.getColumnIndexOrThrow("themePaletteJson"))
                    )
                    assertEquals(song.artworkPath, cursor.getString(cursor.getColumnIndexOrThrow("artworkPath")))
                    assertEquals(song.localPath, cursor.getString(cursor.getColumnIndexOrThrow("localPath")))
                    assertEquals(song.transferredAt, cursor.getLong(cursor.getColumnIndexOrThrow("transferredAt")))
                }
                assertFalse(cursor.moveToNext())
            }

            db.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' " +
                    "AND name IN ('local_playlists', 'local_playlist_songs')"
            ).use { cursor ->
                val tables = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    tables += cursor.getString(0)
                }
                assertEquals(setOf("local_playlists", "local_playlist_songs"), tables)
            }

            db.execSQL(
                "INSERT INTO local_playlists (playlistId, name, importedAt) VALUES (?, ?, ?)",
                arrayOf<Any>("phone-playlist-7", "Morning Walk", 1_780_000_001_234L)
            )
            val song = songs.first()
            db.execSQL(
                "INSERT INTO local_playlist_songs (playlistId, position, songId, title, artist) " +
                    "VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any>("phone-playlist-7", 3, song.songId, song.title, song.artist)
            )

            db.query("SELECT playlistId, name, importedAt FROM local_playlists").use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("phone-playlist-7", cursor.getString(0))
                assertEquals("Morning Walk", cursor.getString(1))
                assertEquals(1_780_000_001_234L, cursor.getLong(2))
            }
            db.query(
                "SELECT playlistId, position, songId, title, artist FROM local_playlist_songs"
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("phone-playlist-7", cursor.getString(0))
                assertEquals(3, cursor.getInt(1))
                assertEquals(song.songId, cursor.getString(2))
                assertEquals(song.title, cursor.getString(3))
                assertEquals(song.artist, cursor.getString(4))
            }
        }
    }

    @Test
    fun migratedDatabaseOpensWithRoomAndReadsSongsThroughDao() = runBlocking {
        createVersion5Database()
        helper.runMigrationsAndValidate(
            TEST_DB,
            6,
            true,
            *WearMusicDatabase.ALL_MIGRATIONS
        ).close()

        val db = Room.databaseBuilder(context, WearMusicDatabase::class.java, TEST_DB)
            .addMigrations(*WearMusicDatabase.ALL_MIGRATIONS)
            .build()
        try {
            assertEquals(songs, db.localSongDao().getAllSongs().first())
        } finally {
            db.close()
        }
    }

    private fun createVersion5Database() {
        helper.createDatabase(TEST_DB, 5).use { db ->
            for (song in songs) {
                db.execSQL(
                    """
                        INSERT INTO local_songs (
                            songId, title, artist, album, albumId, duration, mimeType,
                            fileSize, bitrate, sampleRate, isFavorite, favoriteSyncPending,
                            paletteSeedArgb, themePaletteJson, artworkPath, localPath, transferredAt
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(
                        song.songId, song.title, song.artist, song.album, song.albumId,
                        song.duration, song.mimeType, song.fileSize, song.bitrate, song.sampleRate,
                        if (song.isFavorite) 1 else 0,
                        if (song.favoriteSyncPending) 1 else 0,
                        song.paletteSeedArgb, song.themePaletteJson, song.artworkPath,
                        song.localPath, song.transferredAt
                    )
                )
            }
        }
    }

    companion object {
        private const val TEST_DB = "wear-migration-test-5-to-6.db"
    }
}
