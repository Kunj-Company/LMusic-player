package com.twilighty.lmusic.data.provider

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SharedArtworkContentProviderTest {

    @Test
    fun buildSongUri_usesDedicatedArtworkAuthority() {
        val uri = SharedArtworkContentProvider.buildSongUriString(
            packageName = "com.twilighty.lmusic",
            songId = 42L
        )

        assertThat(uri).isEqualTo("content://com.twilighty.lmusic.artwork/song/42")
    }

    @Test
    fun buildSongUri_preservesCacheBustToken() {
        val uri = SharedArtworkContentProvider.buildSongUriString(
            packageName = "com.twilighty.lmusic",
            songId = 42L,
            cacheBustToken = "1234"
        )

        assertThat(uri)
            .isEqualTo("content://com.twilighty.lmusic.artwork/song/42?t=1234")
    }

    @Test
    fun parseSongId_rejectsOtherAuthorities() {
        val songId = SharedArtworkContentProvider.parseSongId(
            uriString = "content://example.com.artwork/song/42",
            packageName = "com.twilighty.lmusic"
        )

        assertThat(songId).isNull()
    }

    @Test
    fun parseSongId_readsSharedArtworkSongUri() {
        val songId = SharedArtworkContentProvider.parseSongId(
            uriString = "content://com.twilighty.lmusic.artwork/song/42",
            packageName = "com.twilighty.lmusic"
        )

        assertThat(songId).isEqualTo(42L)
    }
}
