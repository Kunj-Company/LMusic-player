package com.twilighty.lmusic.presentation.ytmusic.dashboard

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.twilighty.lmusic.data.ytmusic.YouTubeMusicUris
import com.twilighty.lmusic.presentation.streaming.StreamingDashboardScreen
import com.twilighty.lmusic.presentation.viewmodel.PlayerViewModel
import com.twilighty.lmusic.presentation.ytmusic.auth.YouTubeMusicLoginActivity
import com.twilighty.lmusic.presentation.ytmusic.video.YouTubeMusicVideoActivity

@Composable
fun YouTubeMusicDashboardScreen(
    onBack: () -> Unit,
    playerViewModel: PlayerViewModel,
    viewModel: YouTubeMusicDashboardViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    StreamingDashboardScreen(
        viewModel = viewModel,
        playerViewModel = playerViewModel,
        toSong = YouTubeMusicUris::toSong,
        searchLabel = "Search or paste a YouTube link",
        signInBody = "Open your library and playlists. Feed, search, play, and download work without an account.",
        onSignIn = { context.startActivity(Intent(context, YouTubeMusicLoginActivity::class.java)) },
        onBack = onBack,
        onWatch = { track -> context.startActivity(YouTubeMusicVideoActivity.intent(context, track.videoId, track.title)) },
    )
}
