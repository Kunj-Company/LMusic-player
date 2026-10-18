package com.twilighty.lmusic.data.gestures

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GestureDetectorsTest {

    private val g = STANDARD_GRAVITY

    @Nested
    inner class Shake {
        private fun detector() = ShakeDetector(thresholdMs2 = 2.5f * g)

        /** Feeds a peak (high sample then low sample) and returns whether the high sample fired. */
        private fun ShakeDetector.peak(t: Long, magnitude: Float = 3f * g): Boolean {
            val fired = onSample(magnitude, t)
            onSample(0.2f * g, t + 40)
            return fired
        }

        @Test
        fun `two peaks within the window fire`() {
            val d = detector()
            assertFalse(d.peak(0))
            assertTrue(d.peak(300))
        }

        @Test
        fun `a single peak does not fire`() {
            val d = detector()
            assertFalse(d.peak(0))
            assertFalse(d.onSample(0f, 2_000))
        }

        @Test
        fun `peaks further apart than 600 ms do not fire`() {
            val d = detector()
            assertFalse(d.peak(0))
            assertFalse(d.peak(700))
            // ...but the second peak starts a new window.
            assertTrue(d.peak(1_100))
        }

        @Test
        fun `a sustained jolt counts as one peak`() {
            val d = detector()
            assertFalse(d.onSample(3f * g, 0))
            assertFalse(d.onSample(3.2f * g, 50))
            assertFalse(d.onSample(2.9f * g, 100))
            assertFalse(d.onSample(2f * g, 150)) // below threshold but above re-arm level
            assertFalse(d.onSample(3f * g, 200)) // still not re-armed
        }

        @Test
        fun `below threshold never fires`() {
            val d = detector()
            for (t in 0L..1_000L step 20) {
                assertFalse(d.onSample(if ((t / 20) % 2 == 0L) 2.4f * g else 0f, t))
            }
        }

        @Test
        fun `cooldown blocks a second shake for 2 seconds`() {
            val d = detector()
            d.peak(0)
            assertTrue(d.peak(200))
            assertFalse(d.peak(600))
            assertFalse(d.peak(800))
            assertFalse(d.peak(2_100)) // still inside the cooldown (1.9 s after the shake)
            assertFalse(d.peak(2_300)) // first peak after the cooldown
            assertTrue(d.peak(2_500))
        }

        @Test
        fun `sensitivity changes the threshold`() {
            val d = ShakeDetector(thresholdMs2 = ShakeSensitivity.HIGH.thresholdMs2)
            assertFalse(d.peak(0, 2.1f * g))
            assertTrue(d.peak(200, 2.1f * g))

            val low = ShakeDetector(thresholdMs2 = ShakeSensitivity.LOW.thresholdMs2)
            assertFalse(low.peak(0, 2.6f * g))
            assertFalse(low.peak(200, 2.6f * g))
        }

        @Test
        fun `reset clears pending peaks`() {
            val d = detector()
            d.peak(0)
            d.reset()
            assertFalse(d.peak(200))
        }

        @Test
        fun `NaN is ignored`() {
            assertFalse(detector().onSample(Float.NaN, 0))
        }
    }

    @Nested
    inner class FaceDown {
        private val up = 9.8f
        private val down = -9.7f

        /** Feeds [z] every 50 ms from [from] until [to] inclusive; returns the events seen. */
        private fun FaceDownDetector.feed(z: Float, from: Long, to: Long, near: Boolean? = null) =
            (from..to step 50).mapNotNull { onSample(z, it, near) }

        @Test
        fun `face-down for 700 ms pauses once`() {
            val d = FaceDownDetector()
            d.feed(up, 0, 500)
            val events = d.feed(down, 550, 3_000)
            assertEquals(listOf(FaceDownDetector.Event.FACE_DOWN), events)
            assertTrue(d.isFaceDown)
        }

        @Test
        fun `fires only after the hold time`() {
            val d = FaceDownDetector()
            d.feed(up, 0, 100)
            assertTrue(d.feed(down, 150, 800).isEmpty()) // 650 ms
            assertEquals(listOf(FaceDownDetector.Event.FACE_DOWN), d.feed(down, 850, 900))
        }

        @Test
        fun `a brief flip does not fire`() {
            val d = FaceDownDetector()
            d.feed(up, 0, 100)
            d.feed(down, 150, 600)
            d.feed(up, 650, 700)
            assertTrue(d.feed(down, 750, 1_400).isEmpty())
        }

        @Test
        fun `already face-down when listening starts does not fire`() {
            val d = FaceDownDetector()
            assertTrue(d.feed(down, 0, 5_000).isEmpty())
        }

        @Test
        fun `z just above the threshold does not fire`() {
            val d = FaceDownDetector()
            d.feed(up, 0, 100)
            assertTrue(d.feed(-8.0f, 150, 3_000).isEmpty())
        }

        @Test
        fun `proximity far blocks, unknown proximity is ignored`() {
            val far = FaceDownDetector()
            far.feed(up, 0, 100)
            assertTrue(far.feed(down, 150, 2_000, near = false).isEmpty())

            val near = FaceDownDetector()
            near.feed(up, 0, 100)
            assertEquals(listOf(FaceDownDetector.Event.FACE_DOWN), near.feed(down, 150, 2_000, near = true))
        }

        @Test
        fun `turning face-up after face-down fires FACE_UP`() {
            val d = FaceDownDetector()
            d.feed(up, 0, 100)
            d.feed(down, 150, 1_000)
            val events = d.feed(up, 1_050, 2_000)
            assertEquals(listOf(FaceDownDetector.Event.FACE_UP), events)
            assertFalse(d.isFaceDown)
        }

        @Test
        fun `face-up without a prior face-down never fires`() {
            val d = FaceDownDetector()
            assertTrue(d.feed(up, 0, 3_000).isEmpty())
        }

        @Test
        fun `can pause again after being turned back up`() {
            val d = FaceDownDetector()
            d.feed(up, 0, 100)
            d.feed(down, 150, 1_000)
            d.feed(up, 1_050, 1_600)
            assertEquals(listOf(FaceDownDetector.Event.FACE_DOWN), d.feed(down, 1_650, 2_600))
        }

        @Test
        fun `reset keeping face-down still allows FACE_UP`() {
            val d = FaceDownDetector()
            d.feed(up, 0, 100)
            d.feed(down, 150, 1_000)
            d.reset(keepFaceDown = true)
            assertEquals(listOf(FaceDownDetector.Event.FACE_UP), d.feed(up, 5_000, 6_000))
        }

        @Test
        fun `full reset forgets face-down`() {
            val d = FaceDownDetector()
            d.feed(up, 0, 100)
            d.feed(down, 150, 1_000)
            d.reset()
            assertTrue(d.feed(up, 5_000, 6_000).isEmpty())
        }

        @Test
        fun `NaN is ignored`() {
            assertNull(FaceDownDetector().onSample(Float.NaN, 0))
        }
    }

    @Nested
    inner class Gravity {
        @Test
        fun `steady gravity yields no linear acceleration`() {
            val f = GravityFilter()
            var last = 0f
            repeat(50) { last = f.update(0f, 0f, g) }
            assertTrue(last < 0.01f)
            assertTrue(f.gravity[2] > g - 0.01f)
        }

        @Test
        fun `a sudden jolt shows up as linear acceleration`() {
            val f = GravityFilter()
            repeat(50) { f.update(0f, 0f, g) }
            assertTrue(f.update(30f, 0f, g) > 20f)
        }
    }
}
