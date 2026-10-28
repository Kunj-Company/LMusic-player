package com.twilighty.lmusic.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.ambient.AmbientLifecycleObserver
import com.twilighty.lmusic.data.WearLifecycleState
import com.twilighty.lmusic.presentation.theme.WearLMusicTheme
import com.twilighty.lmusic.presentation.viewmodel.WearPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WearMainActivity : FragmentActivity() {

    companion object {
        val isForeground: Boolean
            get() = WearLifecycleState.isForeground.value
    }

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            WearLifecycleState.setAmbient(true)
        }

        override fun onExitAmbient() {
            WearLifecycleState.setAmbient(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AmbientLifecycleObserver(this, ambientCallback).also {
            lifecycle.addObserver(it)
        }

        setContent {
            val playerViewModel: WearPlayerViewModel = hiltViewModel()
            val albumArt by playerViewModel.albumArt.collectAsStateWithLifecycle()
            val paletteSeedArgb by playerViewModel.paletteSeedArgb.collectAsStateWithLifecycle()
            val themePalette by playerViewModel.themePalette.collectAsStateWithLifecycle()

            WearLMusicTheme(
                albumArt = albumArt,
                seedColorArgb = paletteSeedArgb,
                themePalette = themePalette,
            ) {
                WearNavigation()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        WearLifecycleState.setForeground(true)
    }

    override fun onStop() {
        WearLifecycleState.setForeground(false)
        super.onStop()
    }
}
