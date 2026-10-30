package com.twilighty.lmusic.data.model

enum class StorageFilter(val value: Int) {
    ALL(0),
    OFFLINE(1),
    ONLINE(2),
    /** Phone files saved from YouTube Music or JioSaavn (Music/LMusic/YouTube Music, Music/LMusic/JioSaavn). */
    DOWNLOADED(3)
}
