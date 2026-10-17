package com.twilighty.lmusic.data.crash

import android.content.Context
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Safe mode: entered automatically after two launches in a row that crashed (or died) before
 * becoming stable. While active, optional / experimental features must stay off.
 *
 * How a feature checks it (synchronous, cheap after the first call, safe on any thread):
 *
 * ```
 * if (SafeMode.isActive(context)) return   // don't autostart / schedule / attach listeners
 * ```
 *
 * Features that start at app launch (or autostart from a screen) should skip their autostart
 * while it is active. They do not need to change their own saved setting: the app already turned
 * Smart features off when safe mode began; the user turns things back on in Settings after
 * tapping "Turn features back on". To react to that tap without a restart, register with
 * [addExitListener].
 *
 * Storage: SharedPreferences "lmusic_safe_mode" (key "active"), written with commit().
 */
object SafeMode {
    private const val PREFS = "lmusic_safe_mode"
    private const val KEY_ACTIVE = "active"
    private const val KEY_UNSTABLE_LAUNCHES = "unstable_launches"
    private const val KEY_ENTERED_AT = "entered_at"

    /** Consecutive unstable launches (before this one) that trigger safe mode. */
    private const val THRESHOLD = 2

    @Volatile
    private var cachedActive: Boolean? = null

    /** True when safe mode was entered during this process start. */
    @Volatile
    var enteredThisLaunch: Boolean = false
        private set

    private val exitListeners = CopyOnWriteArrayList<() -> Unit>()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Whether optional features must stay off. Never throws. */
    fun isActive(context: Context): Boolean {
        cachedActive?.let { return it }
        val value = runCatching { prefs(context).getBoolean(KEY_ACTIVE, false) }.getOrDefault(false)
        cachedActive = value
        return value
    }

    /**
     * Called first thing in Application.onCreate. Counts this launch as unstable (until
     * [markStable]) and enters safe mode if the previous [THRESHOLD] launches never became stable.
     * Returns whether safe mode is active for this launch.
     */
    fun onAppStart(context: Context): Boolean {
        val p = prefs(context)
        val previous = p.getInt(KEY_UNSTABLE_LAUNCHES, 0)
        val alreadyActive = p.getBoolean(KEY_ACTIVE, false)
        val enter = !alreadyActive && previous >= THRESHOLD
        val editor = p.edit().putInt(KEY_UNSTABLE_LAUNCHES, if (enter) 1 else previous + 1)
        if (enter) {
            editor.putBoolean(KEY_ACTIVE, true).putLong(KEY_ENTERED_AT, System.currentTimeMillis())
        }
        editor.commit()
        enteredThisLaunch = enter
        val active = alreadyActive || enter
        cachedActive = active
        return active
    }

    /** This launch is stable (MainActivity resumed ~10 s, or the process lived long enough). */
    fun markStable(context: Context) {
        runCatching {
            val p = prefs(context)
            if (p.getInt(KEY_UNSTABLE_LAUNCHES, 0) != 0) p.edit().putInt(KEY_UNSTABLE_LAUNCHES, 0).apply()
        }
    }

    /** "Turn features back on": clears the flag. Features are re-enabled by the user in Settings. */
    fun exit(context: Context) {
        runCatching {
            prefs(context).edit()
                .putBoolean(KEY_ACTIVE, false)
                .putInt(KEY_UNSTABLE_LAUNCHES, 0)
                .remove(KEY_ENTERED_AT)
                .commit()
        }
        cachedActive = false
        enteredThisLaunch = false
        exitListeners.forEach { runCatching { it() } }
    }

    fun addExitListener(listener: () -> Unit) {
        exitListeners += listener
    }
}
