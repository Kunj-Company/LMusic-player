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
 * resume when turned face-up). Both work with the screen off / locked while music plays.
 *
 * Battery: sensors are registered only while a gesture is enabled AND music is playing (plus a
 * short [PAUSE_GRACE_MS] after it stops, or while a face-down pause waits to resume) and are
 * unregistered otherwise. Only the plain accelerometer is used with the screen off (no gyro
 * fusion sensors), at a low rate with hardware batching:
 * - shake, screen on: linear acceleration (or accelerometer) at SENSOR_DELAY_UI;
 * - shake, screen off: accelerometer at ~50 Hz. The wake-up variant (batched, [SHAKE_OFF_LATENCY_US])
 *   is used when it has a FIFO, because with audio offload the CPU sleeps during playback and a
 *   non-wake-up sensor would deliver nothing. A non-wake-up proximity sensor runs alongside so
 *   shakes are ignored in a pocket or bag, and a firmer double shake is required;
 * - face-down: accelerometer at SENSOR_DELAY_NORMAL, batched ~1 s with the screen off.
 * Nothing runs in safe mode. MusicService is a mediaPlayback foreground service while playing,
 * so background sensor access is allowed.
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

    /** How a sensor is registered; a change means unregister + register again. */
    private data class Registration(val sensor: Sensor, val screenOn: Boolean)

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
    private val pocketShakeDetector = PocketShakeDetector()
    private val faceDownDetector = FaceDownDetector()
    private val shakeGravityFilter = GravityFilter()

    private var shakeRegistration: Registration? = null
    private var faceRegistration: Registration? = null
    private var proximityRegistered = false
    private var screenReceiverRegistered = false
    private var screenOn = true
    private var proximityNear: Boolean? = null

    /** Set when we paused because the phone was turned face-down; cleared on any manual play. */
    private var pausedByFaceDownAtMs: Long? = null

    /** When playback last stopped (for the short grace before sensors stop). */
    private var stoppedAtMs: Long? = null

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
                    pocketShakeDetector.thresholdMs2 = newSettings.shakeSensitivity.screenOffThresholdMs2
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
        stoppedAtMs = null
        settings = GestureSettings()
        unregisterShake()
        unregisterFace(keepFaceDown = false)
        unregisterProximity()
        unregisterScreenReceiver()
    }

    fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            pausedByFaceDownAtMs = null
            stoppedAtMs = null
        } else if (stoppedAtMs == null) {
            stoppedAtMs = SystemClock.elapsedRealtime()
        }
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

    /** Milliseconds left in the after-pause grace, or 0 when it is over / not stopped. */
    private fun graceRemainingMs(): Long {
        val stoppedAt = stoppedAtMs ?: return 0L
        val remaining = PAUSE_GRACE_MS - (SystemClock.elapsedRealtime() - stoppedAt)
        if (remaining <= 0L) {
            stoppedAtMs = null
            return 0L
        }
        return remaining
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
            if (isPlaying) stoppedAtMs = null
            val grace = if (isPlaying) 0L else graceRemainingMs()
            val active = isPlaying || grace > 0L
            val waiting = waitingToResume()
            val couldUseSensors = attached && !safe && settings.anySensorGesture && (active || waiting)

            if (couldUseSensors) registerScreenReceiver() else unregisterScreenReceiver()

            val wantShake = couldUseSensors && settings.shakeToShuffle && active
            // While a face-down pause waits to resume, the accelerometer stays registered (also
            // with the screen off) for at most RESUME_WINDOW_MS.
            val wantFace = couldUseSensors && settings.faceDownToPause && (active || waiting)

            if (wantShake) registerShake() else unregisterShake()
            if (wantFace) registerFace() else unregisterFace(keepFaceDown = waiting)
            // Proximity: pocket check for screen-off shakes; "screen against something" for face-down.
            if (wantFace || (wantShake && !screenOn)) registerProximity() else unregisterProximity()

            mainHandler.removeCallbacks(evaluateRunnable)
            val recheckIn = buildList {
                if (grace > 0L) add(grace)
                if (waiting) {
                    val pausedAt = pausedByFaceDownAtMs ?: 0L
                    add(RESUME_WINDOW_MS - (SystemClock.elapsedRealtime() - pausedAt))
                }
            }.minOrNull()
            // Re-check when the grace / resume window ends, so the sensors stop.
            if (recheckIn != null) mainHandler.postDelayed(evaluateRunnable, recheckIn.coerceAtLeast(500L))
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

    // ------------------------------------------------------------------------------ sensors

    /**
     * The accelerometer for screen-off use: the wake-up variant when it can batch at least
     * [minFifoEvents] (so it wakes the CPU a few times a second, not per sample), else the
     * normal one (delivers only while the CPU is awake).
     */
    private fun screenOffAccelerometer(sm: SensorManager, minFifoEvents: Int): Pair<Sensor, Boolean>? {
        val wakeUp = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)
        if (wakeUp != null && wakeUp.fifoMaxEventCount >= minFifoEvents) return wakeUp to true
        val normal = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return null
        return normal to false
    }

    // ------------------------------------------------------------------------------ shake

    private var shakeUsesLinear = true

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val v = event.values
            if (v.size < 3) return
            val registration = shakeRegistration ?: return
            val magnitude = if (shakeUsesLinear) {
                sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
            } else {
                shakeGravityFilter.update(v[0], v[1], v[2])
            }
            val timeMs = event.timestamp / 1_000_000L
            val fired = if (registration.screenOn) {
                shakeDetector.onSample(magnitude, timeMs)
            } else {
                pocketShakeDetector.onSample(magnitude, timeMs, proximityNear)
            }
            if (fired) onShake(screenOffShake = !registration.screenOn)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun registerShake() {
        val sm = sensorManager ?: return
        val wanted: Registration
        val samplingUs: Int
        val latencyUs: Int
        val linear: Boolean
        if (screenOn) {
            val linearSensor = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            val sensor = linearSensor ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
            wanted = Registration(sensor, screenOn = true)
            samplingUs = SensorManager.SENSOR_DELAY_UI
            latencyUs = 0
            linear = linearSensor != null
        } else {
            val minFifo = SHAKE_OFF_LATENCY_US / SHAKE_OFF_SAMPLING_US + 5
            val (sensor, wakeUp) = screenOffAccelerometer(sm, minFifo) ?: return
            wanted = Registration(sensor, screenOn = false)
            samplingUs = SHAKE_OFF_SAMPLING_US
            // Non-wake-up: no batching, the CPU is awake whenever its events arrive anyway.
            latencyUs = if (wakeUp) SHAKE_OFF_LATENCY_US else 0
            linear = false
        }
        if (shakeRegistration == wanted) return
        unregisterShake()
        shakeUsesLinear = linear
        shakeGravityFilter.reset()
        shakeDetector.reset()
        pocketShakeDetector.reset()
        val ok = runCatching { sm.registerListener(shakeListener, wanted.sensor, samplingUs, latencyUs) }
            .getOrDefault(false)
        shakeRegistration = if (ok) wanted else null
    }

    private fun unregisterShake() {
        if (shakeRegistration == null) return
        runCatching { sensorManager?.unregisterListener(shakeListener) }
        shakeRegistration = null
        shakeDetector.reset()
        pocketShakeDetector.reset()
    }

    private fun onShake(screenOffShake: Boolean) {
        // A late sample from the other mode (screen just turned on/off) must not count.
        if (screenOffShake == screenOn) return
        if (screenOffShake && proximityNear == true) return
        val player = host?.currentPlayer() ?: return
        if (!player.isPlaying || player.mediaItemCount < 2) return
        // Felt with the screen off too (MusicService is a foreground service while playing).
        haptics().celebration()
        if (!screenOffShake) showToast("Shuffled")
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

    private val faceListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val v = event.values
            if (v.size < 3) return
            // Raw accelerometer: the detector itself requires the vector to be still (≈ gravity).
            when (faceDownDetector.onSample(v[0], v[1], v[2], event.timestamp / 1_000_000L, proximityNear)) {
                FaceDownDetector.Event.FACE_DOWN -> onFaceDown()
                FaceDownDetector.Event.FACE_UP -> onFaceUp()
                null -> Unit
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun registerFace() {
        val sm = sensorManager ?: return
        val sensor: Sensor
        val samplingUs: Int
        val latencyUs: Int
        if (screenOn) {
            sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
            samplingUs = SensorManager.SENSOR_DELAY_UI
            latencyUs = FACE_ON_LATENCY_US
        } else {
            // SENSOR_DELAY_NORMAL is ~5 Hz: ~5 events per 1 s batch.
            sensor = screenOffAccelerometer(sm, minFifoEvents = 10)?.first ?: return
            samplingUs = SensorManager.SENSOR_DELAY_NORMAL
            latencyUs = FACE_OFF_LATENCY_US
        }
        val wanted = Registration(sensor, screenOn)
        if (faceRegistration == wanted) return
        val keep = faceDownDetector.isFaceDown && waitingToResume()
        if (faceRegistration != null) {
            runCatching { sm.unregisterListener(faceListener) }
            faceRegistration = null
        }
        // A mode switch (screen on/off) must not lose a pending "resume when face-up".
        faceDownDetector.reset(keepFaceDown = keep)
        val ok = runCatching { sm.registerListener(faceListener, sensor, samplingUs, latencyUs) }
            .getOrDefault(false)
        faceRegistration = if (ok) wanted else null
    }

    private fun unregisterFace(keepFaceDown: Boolean) {
        if (faceRegistration == null) return
        runCatching { sensorManager?.unregisterListener(faceListener) }
        faceRegistration = null
        faceDownDetector.reset(keepFaceDown = keepFaceDown)
    }

    private fun onFaceDown() {
        val player = host?.currentPlayer() ?: return
        if (!player.isPlaying) return
        // A clear double tap, so it is felt through a table with the screen off.
        haptics().success()
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

    // ------------------------------------------------------------------------------ proximity

    private val proximityListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val distance = event.values.firstOrNull() ?: return
            val range = event.sensor.maximumRange
            val near = distance < range && distance < PROXIMITY_NEAR_CM
            if (near) pocketShakeDetector.reset()
            proximityNear = near
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun registerProximity() {
        if (proximityRegistered) return
        val sm = sensorManager ?: return
        proximityNear = null
        // Prefer the non-wake-up variant; the default one is wake-up but on-change, so still cheap.
        val proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY, false)
            ?: sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            ?: return
        proximityRegistered = runCatching {
            sm.registerListener(proximityListener, proximity, SensorManager.SENSOR_DELAY_NORMAL)
        }.getOrDefault(false)
    }

    private fun unregisterProximity() {
        if (!proximityRegistered) return
        runCatching { sensorManager?.unregisterListener(proximityListener) }
        proximityRegistered = false
        proximityNear = null
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
        // Only resume if flipped back soon; after this the user presses play themselves.
        const val RESUME_WINDOW_MS = 2 * 60 * 1000L
        // Keep listening briefly after playback stops (track changes, buffering).
        const val PAUSE_GRACE_MS = 3_000L
        const val PROXIMITY_NEAR_CM = 5f
        const val FACE_ON_LATENCY_US = 100_000
        const val FACE_OFF_LATENCY_US = 1_000_000
        const val SHAKE_OFF_SAMPLING_US = 20_000 // 50 Hz
        const val SHAKE_OFF_LATENCY_US = 250_000
    }
}
