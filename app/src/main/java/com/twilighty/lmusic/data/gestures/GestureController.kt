package com.twilighty.lmusic.data.gestures

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.widget.Toast
import androidx.media3.common.Player
import com.twilighty.lmusic.data.crash.SafeMode
import com.twilighty.lmusic.presentation.haptics.LMusicHaptics
import com.twilighty.lmusic.presentation.haptics.LMusicHapticsEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Phone-motion gestures for playback: shake to shuffle, turn face-down to pause (and optionally
 * resume when turned face-up).
 *
 * Battery: sensors are registered only while a gesture is enabled AND music is playing (or a
 * face-down pause is waiting to resume) AND the screen is on, and are unregistered right away
 * otherwise. Nothing runs in safe mode. The screen on/off receiver is only registered while
 * playback could use the sensors.
 *
 * Lifecycle: [attach] from MusicService.onCreate, [onIsPlayingChanged] from the player listener,
 * [detach] in onDestroy. Everything runs on the main thread. The constructor does no work.
 */
@Singleton
class GestureController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: GesturePreferences,
    private val hapticsEngine: dagger.Lazy<LMusicHapticsEngine>,
) {
    /** What the controller needs from the playback service. */
    interface Host {
        /** The player currently published to the media session. */
        fun currentPlayer(): Player?

        /** Turns the app's shuffle on/off the same way the shuffle button does (syncs the UI). */
        fun setShuffleEnabled(enabled: Boolean)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var scope: CoroutineScope? = null
    private var host: Host? = null
    private var settings = GestureSettings()

    private val sensorManager: SensorManager? by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    private val powerManager: PowerManager? by lazy {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }

    private val shakeDetector = ShakeDetector()
    private val faceDownDetector = FaceDownDetector()
    private val shakeGravityFilter = GravityFilter()
    private val faceGravityFilter = GravityFilter(alpha = 0.9f)

    private var shakeRegistered = false
    private var faceRegistered = false
    private var proximityRegistered = false
    private var screenReceiverRegistered = false
    private var screenOn = true
    private var proximityNear: Boolean? = null

    /** Set when we paused because the phone was turned face-down; cleared on any manual play. */
    private var pausedByFaceDownAtMs: Long? = null

    private val evaluateRunnable = Runnable { evaluate() }

    // ------------------------------------------------------------------------------ lifecycle

    fun attach(host: Host) {
        if (this.host != null) detach()
        this.host = host
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        newScope.launch {
            runCatching {
                preferences.settings.collect { newSettings ->
                    settings = newSettings
                    shakeDetector.thresholdMs2 = newSettings.shakeSensitivity.thresholdMs2
                    if (!newSettings.faceDownToPause || !newSettings.resumeWhenFaceUp) pausedByFaceDownAtMs = null
                    evaluate()
                }
            }.onFailure { Timber.tag(TAG).w(it, "Gesture settings unavailable") }
        }
    }

    fun detach() {
        mainHandler.removeCallbacksAndMessages(null)
        scope?.cancel()
        scope = null
        host = null
        pausedByFaceDownAtMs = null
        settings = GestureSettings()
        unregisterShake()
        unregisterFace(keepFaceDown = false)
        unregisterScreenReceiver()
    }

    fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) pausedByFaceDownAtMs = null
        evaluate()
    }

    // ------------------------------------------------------------------------------ state

    private fun waitingToResume(): Boolean {
        val pausedAt = pausedByFaceDownAtMs ?: return false
        if (!settings.faceDownToPause || !settings.resumeWhenFaceUp) return false
        if (SystemClock.elapsedRealtime() - pausedAt > RESUME_WINDOW_MS) {
            pausedByFaceDownAtMs = null
            return false
        }
        return true
    }

    /** Registers / unregisters everything to match the current state. Main thread only. */
    private fun evaluate() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(evaluateRunnable)
            return
        }
        runCatching {
            val attached = host != null
            val safe = SafeMode.isActive(context)
            val isPlaying = host?.currentPlayer()?.isPlaying == true
            val waiting = waitingToResume()
            val couldUseSensors = attached && !safe && settings.anySensorGesture && (isPlaying || waiting)

            if (couldUseSensors) registerScreenReceiver() else unregisterScreenReceiver()

            val wantShake = couldUseSensors && screenOn && settings.shakeToShuffle && isPlaying
            // While a face-down pause waits to resume, the (non-wake-up) gravity sensor stays
            // registered even with the screen off, for at most RESUME_WINDOW_MS (10 min). Its
            // events only arrive while the CPU is awake (batched otherwise), so a flip may be
            // noticed late, but picking the phone up (screen on) delivers them right away.
            val wantFace = couldUseSensors && settings.faceDownToPause &&
                ((screenOn && isPlaying) || waiting)

            if (wantShake) registerShake() else unregisterShake()
            if (wantFace) registerFace() else unregisterFace(keepFaceDown = waiting)

            mainHandler.removeCallbacks(evaluateRunnable)
            if (waiting) {
                // Re-check when the resume window ends, so the sensors stop.
                val pausedAt = pausedByFaceDownAtMs ?: 0L
                val remaining = RESUME_WINDOW_MS - (SystemClock.elapsedRealtime() - pausedAt)
                mainHandler.postDelayed(evaluateRunnable, remaining.coerceAtLeast(1_000L))
            }
        }.onFailure { Timber.tag(TAG).w(it, "Gesture evaluate failed") }
    }

    // ------------------------------------------------------------------------------ screen

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            screenOn = when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> false
                Intent.ACTION_SCREEN_ON -> true
                else -> screenOn
            }
            evaluate()
        }
    }

    private fun registerScreenReceiver() {
        if (screenReceiverRegistered) return
        screenOn = powerManager?.isInteractive ?: true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        runCatching {
            context.registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            screenReceiverRegistered = true
        }.onFailure { Timber.tag(TAG).w(it, "Screen receiver failed") }
    }

    private fun unregisterScreenReceiver() {
        if (!screenReceiverRegistered) return
        runCatching { context.unregisterReceiver(screenReceiver) }
        screenReceiverRegistered = false
    }

    // ------------------------------------------------------------------------------ shake

    private var shakeUsesLinear = true

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val v = event.values
            if (v.size < 3) return
            val magnitude = if (shakeUsesLinear) {
                sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
            } else {
                shakeGravityFilter.update(v[0], v[1], v[2])
            }
            if (shakeDetector.onSample(magnitude, event.timestamp / 1_000_000L)) onShake()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun registerShake() {
        if (shakeRegistered) return
        val sm = sensorManager ?: return
        val linear = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val sensor = linear ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        shakeUsesLinear = linear != null
        shakeGravityFilter.reset()
        shakeDetector.reset()
        shakeRegistered = sm.registerListener(shakeListener, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun unregisterShake() {
        if (!shakeRegistered) return
        runCatching { sensorManager?.unregisterListener(shakeListener) }
        shakeRegistered = false
        shakeDetector.reset()
    }

    private fun onShake() {
        val player = host?.currentPlayer() ?: return
        if (!player.isPlaying || player.mediaItemCount < 2) return
        haptics().celebration()
        showToast("Shuffled")
        if (!player.shuffleModeEnabled) {
            host?.setShuffleEnabled(true)
            // Give the app a moment to reorder the queue before jumping.
            mainHandler.postDelayed({ skipToRandom() }, SHUFFLE_SETTLE_MS)
        } else {
            skipToRandom()
        }
    }

    private fun skipToRandom() {
        runCatching {
            val player = host?.currentPlayer() ?: return
            val count = player.mediaItemCount
            if (count < 2) return
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            } else {
                val current = player.currentMediaItemIndex
                var target = Random.nextInt(count - 1)
                if (target >= current) target++
                player.seekTo(target.coerceIn(0, count - 1), 0L)
            }
        }.onFailure { Timber.tag(TAG).w(it, "Shake skip failed") }
    }

    // ------------------------------------------------------------------------------ face-down

    private var faceUsesGravity = true

    private val faceListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val v = event.values
            if (v.size < 3) return
            val z = if (faceUsesGravity) {
                v[2]
            } else {
                faceGravityFilter.update(v[0], v[1], v[2])
                faceGravityFilter.gravity[2]
            }
            when (faceDownDetector.onSample(z, event.timestamp / 1_000_000L, proximityNear)) {
                FaceDownDetector.Event.FACE_DOWN -> onFaceDown()
                FaceDownDetector.Event.FACE_UP -> onFaceUp()
                null -> Unit
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val proximityListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val distance = event.values.firstOrNull() ?: return
            val range = event.sensor.maximumRange
            proximityNear = distance < range && distance < PROXIMITY_NEAR_CM
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun registerFace() {
        if (faceRegistered) return
        val sm = sensorManager ?: return
        val gravity = sm.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val sensor = gravity ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        faceUsesGravity = gravity != null
        faceGravityFilter.reset()
        faceRegistered = sm.registerListener(
            faceListener,
            sensor,
            SensorManager.SENSOR_DELAY_UI,
            FACE_MAX_REPORT_LATENCY_US
        )
        // Optional: a non-wake-up proximity sensor confirms the screen is against a surface.
        proximityNear = null
        sm.getDefaultSensor(Sensor.TYPE_PROXIMITY, false)?.let { proximity ->
            proximityRegistered = sm.registerListener(proximityListener, proximity, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun unregisterFace(keepFaceDown: Boolean) {
        if (!faceRegistered && !proximityRegistered) return
        runCatching {
            sensorManager?.unregisterListener(faceListener)
            sensorManager?.unregisterListener(proximityListener)
        }
        faceRegistered = false
        proximityRegistered = false
        proximityNear = null
        faceDownDetector.reset(keepFaceDown = keepFaceDown)
    }

    private fun onFaceDown() {
        val player = host?.currentPlayer() ?: return
        if (!player.isPlaying) return
        haptics().toggleOff()
        // Mark before pausing: pause() can call back into onIsPlayingChanged synchronously.
        if (settings.resumeWhenFaceUp) pausedByFaceDownAtMs = SystemClock.elapsedRealtime()
        player.pause()
        evaluate()
    }

    private fun onFaceUp() {
        if (!waitingToResume()) return
        pausedByFaceDownAtMs = null
        val player = host?.currentPlayer() ?: return
        if (!player.isPlaying) {
            haptics().toggleOn()
            player.play()
        }
        evaluate()
    }

    // ------------------------------------------------------------------------------ helpers

    private fun haptics(): LMusicHaptics =
        LMusicHapticsEngine.current() ?: runCatching<LMusicHaptics> { hapticsEngine.get() }.getOrElse { LMusicHaptics.get() }

    private fun showToast(text: String) {
        runCatching { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }
    }

    private companion object {
        const val TAG = "GestureController"
        const val SHUFFLE_SETTLE_MS = 650L
        const val RESUME_WINDOW_MS = 10 * 60 * 1000L
        const val PROXIMITY_NEAR_CM = 5f
        const val FACE_MAX_REPORT_LATENCY_US = 100_000
    }
}
