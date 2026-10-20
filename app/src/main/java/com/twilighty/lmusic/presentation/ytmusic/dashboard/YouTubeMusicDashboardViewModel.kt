package com.twilighty.lmusic.presentation.ytmusic.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twilighty.lmusic.data.ytmusic.YouTubeMusicDownloader
import com.twilighty.lmusic.data.ytmusic.YouTubeMusicLibrary
import com.twilighty.lmusic.data.ytmusic.YouTubeMusicPlaylist
import com.twilighty.lmusic.data.ytmusic.YouTubeMusicRepository
import com.twilighty.lmusic.data.ytmusic.YouTubeMusicShelf
import com.twilighty.lmusic.data.ytmusic.YouTubeMusicTrack
import com.twilighty.lmusic.data.ytmusic.YouTubeMusicUris
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class YouTubeMusicTab {
    FEED,
    LIBRARY,
    SEARCH,
}

data class YouTubeMusicDashboardState(
    val tab: YouTubeMusicTab = YouTubeMusicTab.FEED,
    val loading: Boolean = false,
    val error: String? = null,
    val feed: List<YouTubeMusicShelf> = emptyList(),
    val library: List<YouTubeMusicShelf> = emptyList(),
    val searchResults: List<YouTubeMusicShelf> = emptyList(),
    val openPlaylistTitle: String? = null,
    val playlistTracks: List<YouTubeMusicTrack> = emptyList(),
    val status: String? = null,
    val loggedOut: Boolean = false,
)

@HiltViewModel
class YouTubeMusicDashboardViewModel @Inject constructor(
    private val repository: YouTubeMusicRepository,
    private val downloader: YouTubeMusicDownloader,
    private val library: YouTubeMusicLibrary,
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = repository.session
        .map { it.isLoggedIn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.isLoggedIn)

    val accountName: StateFlow<String> = repository.session
        .map { it.accountName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.accountName.orEmpty())

    private val _state = MutableStateFlow(YouTubeMusicDashboardState())
    val state: StateFlow<YouTubeMusicDashboardState> = combine(
        _state,
        downloader.status,
    ) { state, downloadStatus ->
        state.copy(status = downloadStatus ?: state.status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YouTubeMusicDashboardState())

    init {
        viewModelScope.launch {
            downloader.status.collect { status ->
                if (status != null && (status.startsWith("Saved") || status.contains("failed", ignoreCase = true))) {
                    delay(3000)
                    downloader.clearStatus()
                    _state.update { it.copy(status = null) }
                }
            }
        }
    }

    fun loadFeed() {
        refresh(YouTubeMusicTab.FEED)
    }

    fun reload() {
        val tab = _state.value.tab
        if (tab == YouTubeMusicTab.SEARCH) return
        if (tab == YouTubeMusicTab.LIBRARY && !repository.isLoggedIn) return
        refresh(tab)
    }

    fun selectTab(tab: YouTubeMusicTab) {
        _state.update { it.copy(tab = tab, openPlaylistTitle = null, error = null) }
        when (tab) {
            YouTubeMusicTab.FEED -> if (_state.value.feed.isEmpty()) refresh(tab)
            YouTubeMusicTab.LIBRARY -> if (_state.value.library.isEmpty() && repository.isLoggedIn) refresh(tab)
            YouTubeMusicTab.SEARCH -> Unit
        }
    }

    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(tab = YouTubeMusicTab.SEARCH, loading = true, error = null, openPlaylistTitle = null) }
            val linkedVideoId = YouTubeMusicUris.videoIdFromLink(trimmed)
            runCatching {
                if (linkedVideoId != null) repository.trackPage(linkedVideoId) else repository.search(trimmed)
            }
                .onSuccess { page ->
                    _state.update { it.copy(loading = false, searchResults = page.shelves) }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.message ?: "Search failed") }
                }
        }
    }

    fun openPlaylist(playlist: YouTubeMusicPlaylist) {
        viewModelScope.launch {
            _state.update {
                it.copy(loading = true, error = null, openPlaylistTitle = playlist.title, playlistTracks = emptyList())
            }
            runCatching { repository.playlist(playlist.playlistId) }
                .onSuccess { page ->
                    val tracks = page.shelves.flatMap { shelf -> shelf.tracks }
                    _state.update { it.copy(loading = false, playlistTracks = tracks) }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.message ?: "Could not open playlist") }
                }
        }
    }

    fun closePlaylist() {
        _state.update { it.copy(openPlaylistTitle = null, playlistTracks = emptyList()) }
    }

    /** Remembers the queue; each song is saved to the library when it starts to play. */
    fun prepareQueue(tracks: List<YouTubeMusicTrack>, start: YouTubeMusicTrack) {
        library.remember(tracks)
        viewModelScope.launch { library.saveIfQueued(start.videoId) }
    }

    fun download(track: YouTubeMusicTrack) {
        downloader.enqueue(listOf(track))
    }

    fun downloadAll(tracks: List<YouTubeMusicTrack>) {
        downloader.enqueue(tracks)
    }

    fun logout() {
        repository.logout()
        _state.value = YouTubeMusicDashboardState(loggedOut = true)
    }

    private fun refresh(tab: YouTubeMusicTab) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, tab = tab) }
            val result = runCatching {
                if (tab == YouTubeMusicTab.LIBRARY) repository.library() else repository.home()
            }
            result.onSuccess { page ->
                _state.update { current ->
                    if (tab == YouTubeMusicTab.LIBRARY) {
                        current.copy(loading = false, library = page.shelves)
                    } else {
                        current.copy(loading = false, feed = page.shelves)
                    }
                }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.message ?: "Could not load YouTube Music") }
            }
        }
    }
}
