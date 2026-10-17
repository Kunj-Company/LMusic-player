package com.twilighty.lmusic.presentation.haptics

import android.content.Context
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.twilighty.lmusic.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How much haptic feedback the app gives. */
enum class HapticsLevel { OFF, SUBTLE, FULL }

/** Semantic haptic events. Call sites say *what happened*, the engine decides how it feels. */
enum class HapticEvent {
    PlayPause,
    Skip,
    SeekTick,
    SeekEdge,
    ScrubStart,
    ScrubEnd,
    VolumeStep,
    ToggleOn,
    ToggleOff,
    LongPress,
    Like,
    Unlike,
    DragLift,
    DragSlot,
    DragDrop,
    SwipeThreshold,
    RefreshTrigger,
    Success,
    Error,
    Selection,
    SheetSnap,
    /** Very light tick for plain buttons. Full level only. */
    Tap,
}

/**
 * The app-wide haptics API. Use [LocalLMusicHaptics] in Compose, or [LMusicHaptics.get] elsewhere.
 */
interface LMusicHaptics {
    fun perform(event: HapticEvent)

    fun playPause() = perform(HapticEvent.PlayPause)
    fun skip() = perform(HapticEvent.Skip)
    fun seekTick() = perform(HapticEvent.SeekTick)
    fun seekEdge() = perform(HapticEvent.SeekEdge)
    fun scrubStart() = perform(HapticEvent.ScrubStart)
    fun scrubEnd() = perform(HapticEvent.ScrubEnd)
    fun volumeStep() = perform(HapticEvent.VolumeStep)
    fun toggle(on: Boolean) = perform(if (on) HapticEvent.ToggleOn else HapticEvent.ToggleOff)
    fun toggleOn() = perform(HapticEvent.ToggleOn)
    fun toggleOff() = perform(HapticEvent.ToggleOff)
    fun longPress() = perform(HapticEvent.LongPress)
    fun like(liked: Boolean) = perform(if (liked) HapticEvent.Like else HapticEvent.Unlike)
    fun dragLift() = perform(HapticEvent.DragLift)
    fun dragSlot() = perform(HapticEvent.DragSlot)
    fun dragDrop() = perform(HapticEvent.DragDrop)
    fun swipeThreshold() = perform(HapticEvent.SwipeThreshold)
    fun refreshTrigger() = perform(HapticEvent.RefreshTrigger)
    fun success() = perform(HapticEvent.Success)
    fun error() = perform(HapticEvent.Error)
    fun selection() = perform(HapticEvent.Selection)
    fun sheetSnap() = perform(HapticEvent.SheetSnap)
    fun tap() = perform(HapticEvent.Tap)

    companion object {
        /** Non-Compose accessor (services, view models, receivers). No-op until the engine is created. */
        fun get(): LMusicHaptics = Delegating

        internal object Delegating : LMusicHaptics {
            override fun perform(event: HapticEvent) {
                LMusicHapticsEngine.instance?.perform(event)
            }
        }
    }
}

/** Provided at the root of MainActivity. Falls back to the singleton engine when not provided. */
val LocalLMusicHaptics = staticCompositionLocalOf<LMusicHaptics> { LMusicHaptics.get() }

/** Shorthand for `LocalLMusicHaptics.current`. */
val lmusicHaptics: LMusicHaptics
    @Composable
    @ReadOnlyComposable
    get() = LocalLMusicHaptics.current

// ---------------------------------------------------------------- Preferences

private val Context.hapticsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lmusic_haptics")

@Singleton
class HapticsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val LEVEL = stringPreferencesKey("level")
        val SEEK_TICKS = booleanPreferencesKey("seek_ticks")
    }

    val level: Flow<HapticsLevel> = context.hapticsDataStore.data
        .map { prefs ->
            prefs[Keys.LEVEL]?.let { raw -> HapticsLevel.entries.firstOrNull { it.name == raw } }
                ?: HapticsLevel.FULL
        }
        .distinctUntilChanged()

    val seekTicks: Flow<Boolean> = context.hapticsDataStore.data
        .map { it[Keys.SEEK_TICKS] ?: true }
        .distinctUntilChanged()

    suspend fun setLevel(level: HapticsLevel) {
        context.hapticsDataStore.edit { it[Keys.LEVEL] = level.name }
    }

    suspend fun setSeekTicks(enabled: Boolean) {
        context.hapticsDataStore.edit { it[Keys.SEEK_TICKS] = enabled }
    }
}

// ---------------------------------------------------------------- Engine

/**
 * Central haptics engine.
 *
 * Order of preference for every event:
 * 1. `VibrationEffect.Composition` primitives (scaled per level) when the device supports them all,
 * 2. a predefined effect (`EFFECT_TICK/CLICK/HEAVY_CLICK/DOUBLE_CLICK`),
 * 3. `View.performHapticFeedback` with a matching constant on the attached root view.
 *
 * Vibrations use `USAGE_TOUCH`, so the system "Touch feedback" setting and intensity still apply.
 * Light ticks are rate limited so drags never turn into a buzz.
 */
@Singleton
class LMusicHapticsEngine @Inject constructor(
    @ApplicationContext context: Context,
    private val preferences: HapticsPreferences,
    private val userPreferencesRepository: UserPreferencesRepository,
) : LMusicHaptics {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val vibrator: Vibrator? = runCatching {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    }.getOrNull()?.takeIf { it.hasVibrator() }

    private val touchAttributes: VibrationAttributes =
        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)

    private var viewRef: WeakReference<View>? = null

    /** Effective level: the legacy master switch (hapticsEnabled) wins when it is off. */
    val level: StateFlow<HapticsLevel> = combine(
        userPreferencesRepository.hapticsEnabledFlow,
        preferences.level
    ) { enabled, level -> if (!enabled) HapticsLevel.OFF else level }
        .stateIn(scope, SharingStarted.Eagerly, HapticsLevel.FULL)

    val seekTicksEnabled: StateFlow<Boolean> = preferences.seekTicks
        .stateIn(scope, SharingStarted.Eagerly, true)

    private val lastEventTime = LongArray(HapticEvent.entries.size)
    private var lastLightTickTime = 0L
    private val supportCache = HashMap<HapticEvent, Boolean>()

    init {
        instance = this
    }

    fun attachView(view: View) {
        viewRef = WeakReference(view)
    }

    fun setLevel(newLevel: HapticsLevel) {
        scope.launch {
            if (newLevel != HapticsLevel.OFF) preferences.setLevel(newLevel)
            userPreferencesRepository.setHapticsEnabled(newLevel != HapticsLevel.OFF)
        }
    }

    fun setSeekTicks(enabled: Boolean) {
        scope.launch { preferences.setSeekTicks(enabled) }
    }

    override fun perform(event: HapticEvent) {
        val currentLevel = level.value
        if (currentLevel == HapticsLevel.OFF) return
        if (!isAllowed(event, currentLevel)) return
        if (!passesRateLimit(event)) return

        val scale = if (currentLevel == HapticsLevel.SUBTLE) SUBTLE_SCALE else 1f
        try {
            if (playComposition(event, scale)) return
            if (playPredefined(event)) return
            playViewFallback(event)
        } catch (_: Throwable) {
            // Haptics must never crash the app.
        }
    }

    private fun isAllowed(event: HapticEvent, level: HapticsLevel): Boolean = when (event) {
        HapticEvent.SeekTick -> level == HapticsLevel.FULL && seekTicksEnabled.value
        HapticEvent.Tap, HapticEvent.SheetSnap, HapticEvent.DragSlot, HapticEvent.VolumeStep ->
            level == HapticsLevel.FULL
        else -> true
    }

    private fun isLightTick(event: HapticEvent) = when (event) {
        HapticEvent.SeekTick, HapticEvent.VolumeStep, HapticEvent.DragSlot,
        HapticEvent.Selection, HapticEvent.SheetSnap, HapticEvent.Tap -> true
        else -> false
    }

    @Synchronized
    private fun passesRateLimit(event: HapticEvent): Boolean {
        val now = SystemClock.uptimeMillis()
        val index = event.ordinal
        // The same event twice within a short window is almost always a double trigger.
        if (now - lastEventTime[index] < SAME_EVENT_WINDOW_MS) return false
        if (isLightTick(event) && now - lastLightTickTime < MIN_TICK_INTERVAL_MS) return false
        lastEventTime[index] = now
        if (isLightTick(event)) lastLightTickTime = now
        return true
    }

    private data class Primitive(val id: Int, val scale: Float, val delayMs: Int = 0)

    private fun primitivesFor(event: HapticEvent): List<Primitive> = when (event) {
        HapticEvent.PlayPause -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f))
        HapticEvent.Skip -> listOf(
            Primitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.25f),
            Primitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.5f)
        )
        HapticEvent.SeekTick -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.35f))
        HapticEvent.SeekEdge -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.55f))
        HapticEvent.ScrubStart -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f))
        HapticEvent.ScrubEnd -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.4f))
        HapticEvent.VolumeStep -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.5f))
        HapticEvent.ToggleOn -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f))
        HapticEvent.ToggleOff -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.8f))
        HapticEvent.LongPress -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.6f))
        HapticEvent.Like -> listOf(
            Primitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.4f),
            Primitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f, delayMs = 30)
        )
        HapticEvent.Unlike -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.5f))
        HapticEvent.DragLift -> listOf(
            Primitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.3f),
            Primitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.7f)
        )
        HapticEvent.DragSlot -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.45f))
        HapticEvent.DragDrop -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.65f))
        HapticEvent.SwipeThreshold -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.55f))
        HapticEvent.RefreshTrigger -> listOf(
            Primitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.3f),
            Primitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f)
        )
        HapticEvent.Success -> listOf(
            Primitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f),
            Primitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, delayMs = 60)
        )
        HapticEvent.Error -> listOf(
            Primitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f),
            Primitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, delayMs = 80),
            Primitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.4f, delayMs = 20)
        )
        HapticEvent.Selection -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f))
        HapticEvent.SheetSnap -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.6f))
        HapticEvent.Tap -> listOf(Primitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.3f))
    }

    private fun playComposition(event: HapticEvent, levelScale: Float): Boolean {
        val vib = vibrator ?: return false
        val primitives = primitivesFor(event)
        val supported = synchronized(supportCache) {
            supportCache.getOrPut(event) {
                vib.areAllPrimitivesSupported(*primitives.map { it.id }.distinct().toIntArray())
            }
        }
        if (!supported) return false
        val composition = VibrationEffect.startComposition()
        primitives.forEach { p ->
            composition.addPrimitive(p.id, (p.scale * levelScale).coerceIn(0.05f, 1f), p.delayMs)
        }
        vib.vibrate(composition.compose(), touchAttributes)
        return true
    }

    private fun predefinedFor(event: HapticEvent): Int = when (event) {
        HapticEvent.SeekTick, HapticEvent.VolumeStep, HapticEvent.DragSlot, HapticEvent.Selection,
        HapticEvent.SheetSnap, HapticEvent.Tap, HapticEvent.ScrubStart, HapticEvent.ToggleOff,
        HapticEvent.Unlike -> VibrationEffect.EFFECT_TICK
        HapticEvent.LongPress, HapticEvent.SeekEdge -> VibrationEffect.EFFECT_HEAVY_CLICK
        HapticEvent.Like, HapticEvent.Success, HapticEvent.Error -> VibrationEffect.EFFECT_DOUBLE_CLICK
        else -> VibrationEffect.EFFECT_CLICK
    }

    private fun playPredefined(event: HapticEvent): Boolean {
        val vib = vibrator ?: return false
        val effect = predefinedFor(event)
        if (vib.areAllEffectsSupported(effect) == Vibrator.VIBRATION_EFFECT_SUPPORT_NO) return false
        vib.vibrate(VibrationEffect.createPredefined(effect), touchAttributes)
        return true
    }

    private fun viewConstantFor(event: HapticEvent): Int = when (event) {
        HapticEvent.SeekTick, HapticEvent.VolumeStep, HapticEvent.Tap -> HapticFeedbackConstants.CLOCK_TICK
        HapticEvent.Selection, HapticEvent.DragSlot, HapticEvent.SheetSnap -> HapticFeedbackConstants.SEGMENT_TICK
        HapticEvent.ToggleOn, HapticEvent.Like -> HapticFeedbackConstants.TOGGLE_ON
        HapticEvent.ToggleOff, HapticEvent.Unlike -> HapticFeedbackConstants.TOGGLE_OFF
        HapticEvent.ScrubStart, HapticEvent.DragLift -> HapticFeedbackConstants.GESTURE_START
        HapticEvent.ScrubEnd, HapticEvent.DragDrop -> HapticFeedbackConstants.GESTURE_END
        HapticEvent.Success, HapticEvent.RefreshTrigger, HapticEvent.SwipeThreshold -> HapticFeedbackConstants.CONFIRM
        HapticEvent.Error -> HapticFeedbackConstants.REJECT
        HapticEvent.LongPress, HapticEvent.SeekEdge -> HapticFeedbackConstants.LONG_PRESS
        HapticEvent.PlayPause, HapticEvent.Skip -> HapticFeedbackConstants.CONTEXT_CLICK
    }

    private fun playViewFallback(event: HapticEvent) {
        val view = viewRef?.get() ?: return
        val constant = viewConstantFor(event)
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            view.performHapticFeedback(constant)
        } else {
            view.post { view.performHapticFeedback(constant) }
        }
    }

    companion object {
        private const val SUBTLE_SCALE = 0.55f
        private const val MIN_TICK_INTERVAL_MS = 35L
        private const val SAME_EVENT_WINDOW_MS = 30L

        @Volatile
        internal var instance: LMusicHapticsEngine? = null
            private set
    }
}
