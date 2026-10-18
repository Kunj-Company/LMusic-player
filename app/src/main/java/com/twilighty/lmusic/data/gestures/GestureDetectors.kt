package com.twilighty.lmusic.data.gestures

/** Standard gravity in m/s². */
const val STANDARD_GRAVITY = 9.80665f

/** How hard a shake must be. Thresholds are peaks of linear acceleration (gravity removed). */
enum class ShakeSensitivity(val label: String, val thresholdG: Float) {
    LOW("Low", 3.0f),
    MEDIUM("Medium", 2.5f),
    HIGH("High", 2.0f);

    val thresholdMs2: Float get() = thresholdG * STANDARD_GRAVITY
}

/**
 * Pure shake detector. Feed it the magnitude of linear acceleration (m/s², gravity removed).
 *
 * A "peak" is a rising edge above [thresholdMs2]; the detector re-arms once the magnitude falls
 * below [rearmFraction] × threshold, so one long jolt counts once. [requiredPeaks] peaks within
 * [windowMs] fire a shake; after that nothing fires for [cooldownMs].
 */
class ShakeDetector(
    var thresholdMs2: Float = ShakeSensitivity.MEDIUM.thresholdMs2,
    private val requiredPeaks: Int = 2,
    private val windowMs: Long = 600L,
    private val cooldownMs: Long = 2_000L,
    private val rearmFraction: Float = 0.5f,
) {
    private val peaks = ArrayDeque<Long>()
    private var armed = true
    private var lastShakeMs: Long? = null

    /** Returns true exactly when a shake is detected. */
    fun onSample(magnitudeMs2: Float, timeMs: Long): Boolean {
        if (magnitudeMs2.isNaN()) return false
        val inCooldown = lastShakeMs?.let { timeMs - it < cooldownMs } ?: false

        if (armed) {
            if (magnitudeMs2 >= thresholdMs2) {
                armed = false
                if (inCooldown) {
                    peaks.clear()
                    return false
                }
                peaks.addLast(timeMs)
                while (peaks.isNotEmpty() && timeMs - peaks.first() > windowMs) peaks.removeFirst()
                if (peaks.size >= requiredPeaks) {
                    peaks.clear()
                    lastShakeMs = timeMs
                    return true
                }
            }
        } else if (magnitudeMs2 < thresholdMs2 * rearmFraction) {
            armed = true
        }
        return false
    }

    fun reset() {
        peaks.clear()
        armed = true
        // Keep the cooldown: re-registering sensors must not allow an instant double shuffle.
    }
}

/**
 * Low-pass gravity estimate for plain accelerometers (used when the device has no
 * TYPE_LINEAR_ACCELERATION / TYPE_GRAVITY sensor).
 */
class GravityFilter(private val alpha: Float = 0.8f) {
    val gravity = FloatArray(3)
    private var initialized = false

    /** Updates the gravity estimate and returns the linear acceleration magnitude. */
    fun update(x: Float, y: Float, z: Float): Float {
        if (!initialized) {
            gravity[0] = x; gravity[1] = y; gravity[2] = z
            initialized = true
        } else {
            gravity[0] = alpha * gravity[0] + (1 - alpha) * x
            gravity[1] = alpha * gravity[1] + (1 - alpha) * y
            gravity[2] = alpha * gravity[2] + (1 - alpha) * z
        }
        val lx = x - gravity[0]
        val ly = y - gravity[1]
        val lz = z - gravity[2]
        return kotlin.math.sqrt(lx * lx + ly * ly + lz * lz)
    }

    fun reset() {
        initialized = false
    }
}

/**
 * Pure face-down debouncer. Feed it the gravity z component (m/s²; +9.8 = screen up, −9.8 =
 * screen down) and, when a proximity sensor exists, whether it reports "near".
 *
 * - [Event.FACE_DOWN] fires once z stays below [faceDownZ] (and proximity, if known, is near) for
 *   [holdMs]. It only fires after the phone was seen not face-down first (so registering while
 *   already face-down never pauses), and fires once per placement.
 * - [Event.FACE_UP] fires after a FACE_DOWN, once z stays above [faceUpZ] for [upHoldMs].
 */
class FaceDownDetector(
    private val faceDownZ: Float = -8.5f,
    private val holdMs: Long = 700L,
    private val faceUpZ: Float = 3.0f,
    private val upHoldMs: Long = 400L,
    private val rearmZ: Float = -5.0f,
) {
    enum class Event { FACE_DOWN, FACE_UP }

    private var armed = false
    private var faceDown = false
    private var downSinceMs: Long? = null
    private var upSinceMs: Long? = null

    val isFaceDown: Boolean get() = faceDown

    fun onSample(gravityZ: Float, timeMs: Long, proximityNear: Boolean? = null): Event? {
        if (gravityZ.isNaN()) return null

        if (gravityZ > rearmZ) {
            armed = true
            downSinceMs = null
        }

        if (faceDown) {
            if (gravityZ > faceUpZ) {
                val since = upSinceMs ?: timeMs.also { upSinceMs = it }
                if (timeMs - since >= upHoldMs) {
                    faceDown = false
                    upSinceMs = null
                    return Event.FACE_UP
                }
            } else {
                upSinceMs = null
            }
            return null
        }

        val downNow = gravityZ < faceDownZ && proximityNear != false
        if (armed && downNow) {
            val since = downSinceMs ?: timeMs.also { downSinceMs = it }
            if (timeMs - since >= holdMs) {
                armed = false
                faceDown = true
                downSinceMs = null
                upSinceMs = null
                return Event.FACE_DOWN
            }
        } else {
            downSinceMs = null
        }
        return null
    }

    /**
     * Sensors were unregistered. Forgets timers; the next placement must start face-up again.
     * With [keepFaceDown] a pending FACE_UP (for "resume when face-up") survives the gap.
     */
    fun reset(keepFaceDown: Boolean = false) {
        armed = false
        if (!keepFaceDown) faceDown = false
        downSinceMs = null
        upSinceMs = null
    }
}
