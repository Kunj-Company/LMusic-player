package com.twilighty.lmusic.data.model

enum class StorageFilter(val value: Int) {
    ALL(0),
    OFFLINE(1),
    ONLINE(2),
    /** Phone files saved from YouTube Music (Music/LMusic/YouTube Music). */
    DOWNLOADED(3)
}
