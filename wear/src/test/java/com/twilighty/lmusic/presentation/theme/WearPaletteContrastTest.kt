package com.twilighty.lmusic.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Checks LMusic's default purple palette against WCAG AA (4.5:1) on each label's fill. */
class WearPaletteContrastTest {

    private val p = DefaultWearPalette
    private val canvas = p.surfaceContainerLowest

    private fun contrast(fg: Color, bg: Color): Float {
        val solidBg = bg.compositeOver(canvas)
        val l1 = fg.compositeOver(solidBg).luminance()
        val l2 = solidBg.luminance()
        return (maxOf(l1, l2) + 0.05f) / (minOf(l1, l2) + 0.05f)
    }

    private fun assertReadable(name: String, fg: Color, bg: Color) {
        val ratio = contrast(fg, bg)
        assertTrue(ratio >= 4.5f, "$name: $ratio:1")
    }

    @Test
    fun `labels are readable on their fills`() {
        assertReadable("text on canvas", p.textPrimary, canvas)
        assertReadable("secondary text on canvas", p.textSecondary, canvas)
        assertReadable("secondary text on cards", p.textSecondary, p.surfaceContainer)
        assertReadable("chip text", p.chipContent, p.chipContainer)
        assertReadable("play icon", p.playContent, p.playContainer)
        assertReadable("transport icons", p.transportContent, p.transportContainer)
        assertReadable("control icons", p.controlContent, p.controlContainer)
        assertReadable("error text", p.textError, canvas)
    }

    @Test
    fun `play button uses the control colours`() {
        assertEquals(p.controlContainer, p.playContainer)
        assertEquals(p.controlContent, p.playContent)
    }
}
