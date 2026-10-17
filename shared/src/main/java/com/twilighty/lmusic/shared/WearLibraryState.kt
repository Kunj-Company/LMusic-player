package com.twilighty.lmusic.shared

import kotlinx.serialization.Serializable

/**
 * Snapshot of songs currently saved on the watch.
 */
@Serializable
data class WearLibraryState(
    val songIds: List<String> = emptyList(),
)
