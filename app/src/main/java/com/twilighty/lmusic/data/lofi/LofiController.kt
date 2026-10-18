package com.twilighty.lmusic.data.lofi

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.twilighty.lmusic.presentation.viewmodel.PlaybackStateHolder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

enum class LofiSource { Generator, Radio }

data class LofiState(
    /** What is loaded; null when lofi is off. */
    val source: LofiSource? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val station: RadioStation? = null,
    /** Stream title from ICY metadata, if the station sends it. */
    val nowPlaying: String? = null,
    /** Wall-clock millis when the sleep timer stops lofi, or null. */
    val sleepTimerEndsAt: Long? = null,
    val error: String? = null,
)

/** Implemented by [LofiService] while it runs. */
internal interface LofiServiceHandle {
    fun pause()
    fun resume()
    fun stopAll()
    fun setSleepTimer(minutes: Int?)
    /** [LofiController.holdForFocus] was cleared: resume the normal idle stop if paused. */
    fun onFocusHoldReleased()
}

/**
 * The public entry point for lofi (generator and radio), also used by Focus sessions.
 *
 * Start methods should be called from the main thread while the app is in the foreground
 * (they start [LofiService]). Nothing heavy happens in the constructor.
 */
@Singleton
class LofiController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val engine: LofiEngine,
    private val preferences: LofiPreferences,
    private val playbackStateHolder: dagger.Lazy<PlaybackStateHolder>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _settings = MutableStateFlow(LofiSettings())
    val settings: StateFlow<LofiSettings> = _settings.asStateFlow()

    private val _state = MutableStateFlow(LofiState())
    val state: StateFlow<LofiState> = _state.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    /** True while lofi (generator or radio) is audibly playing. */
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    @Volatile internal var service: LofiServiceHandle? = null

    /**
     * Set by a running focus session that owns lofi: while true, [LofiService] skips its
     * 10-minute idle self-stop (paused sessions, breaks), so lofi can come back without a
     * background service start. Cleared when the session ends.
     */
    @Volatile
    var holdForFocus: Boolean = false
        set(value) {
            val released = field && !value
            field = value
            if (released) service?.let { s -> runOnMain { s.onFocusHoldReleased() } }
        }
    private var loaded = false
    private var userTouched = false
    private var saveJob: Job? = null

    /** Loads the last-used settings once (cheap, async). Call from UI entry points. */
    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        scope.launch {
            val saved = runCatching { preferences.loadSettings() }.getOrNull()
            if (saved != null && !userTouched) {
                _settings.value = saved
                engine.update(saved)
            }
        }
    }

    // ---------------------------------------------------------------- generator

    /**
     * Starts the generator with [settings]; pauses the main player and takes audio focus.
     * Returns false when Android refused to start the service (e.g. from the background).
     */
    fun start(settings: LofiSettings = _settings.value): Boolean {
        setSettings(settings)
        pauseMainPlayer()
        return startService(LofiService.ACTION_GENERATE)
    }

    fun stop() {
        val s = service
        if (s != null) runOnMain { s.stopAll() } else engine.stop()
    }

    fun pause() { service?.let { s -> runOnMain { s.pause() } } }

    /** Returns false when lofi had to be restarted and Android refused the service start. */
    fun resume(): Boolean {
        val s = service
        return if (s != null && _state.value.source != null) {
            pauseMainPlayer()
            runOnMain { s.resume() }
            true
        } else {
            start()
        }
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) pause() else resume()
    }

    fun setSettings(settings: LofiSettings) {
        userTouched = true
        val n = settings.normalized()
        _settings.value = n
        engine.update(n)
        scheduleSave()
    }

    fun updateSettings(transform: (LofiSettings) -> LofiSettings) = setSettings(transform(_settings.value))

    fun setMood(mood: LofiMood) = updateSettings { it.withMood(mood) }

    /** A new seed: a different tune with the same settings. */
    fun newTune() = updateSettings { it.copy(seed = Random.nextLong(1, Long.MAX_VALUE)) }

    // ---------------------------------------------------------------- radio

    fun playRadio(station: RadioStation) {
        pauseMainPlayer()
        startService(LofiService.ACTION_RADIO) {
            putExtra(LofiService.EXTRA_STATION, json.encodeToString(RadioStation.serializer(), station))
        }
    }

    // ---------------------------------------------------------------- misc

    /** Stop after [minutes] with a fade; null cancels. */
    fun setSleepTimer(minutes: Int?) { service?.let { s -> runOnMain { s.setSleepTimer(minutes) } } }

    /** Soft bell chime. Works whether or not lofi or the main player is playing. */
    fun playChime() = engine.playChime()

    /** Generator level meters for visuals (see [LofiEngine.meters]); null before first start. */
    fun meters(): FloatArray? = engine.meters()

    internal fun publish(newState: LofiState) {
        _state.value = newState
        _isPlaying.value = newState.isPlaying
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(600)
            runCatching { preferences.saveSettings(_settings.value) }
        }
    }

    private fun startService(action: String, extras: Intent.() -> Unit = {}): Boolean {
        val intent = Intent(context, LofiService::class.java).setAction(action).apply(extras)
        return runCatching { ContextCompat.startForegroundService(context, intent) }
            .onFailure {
                Timber.tag("LofiController").w(it, "Could not start lofi service")
                publish(_state.value.copy(error = "Couldn't start lofi"))
            }
            .isSuccess
    }

    /** Pauses the main music player if it is playing (audio focus would do it too). */
    private fun pauseMainPlayer() {
        runOnMain {
            runCatching {
                playbackStateHolder.get().mediaController?.let { if (it.isPlaying) it.pause() }
            }
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
