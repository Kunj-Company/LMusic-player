package com.twilighty.lmusic.data

import com.twilighty.lmusic.shared.WearLibraryItem

data class WearLocalQueueState(
    val items: List<WearLibraryItem> = emptyList(),
    val currentIndex: Int = -1,
)
