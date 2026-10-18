package com.twilighty.lmusic.data.covers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoverGeneratorTest {

    @Test
    fun `same seed gives the same design for every style`() {
        CoverStyle.entries.forEach { style ->
            val request = CoverRequest(key = "In Rainbows", style = style, palette = CoverPaletteChoice.AUTO, variant = 3)
            val a = CoverGenerator.design(request)
            val b = CoverGenerator.design(request.copy())
            assertEquals(a, b, "style $style should be deterministic")
            assertTrue(a.shapes.isNotEmpty(), "style $style should draw something")
        }
    }

    @Test
    fun `seed ignores case and outer spaces of the album name`() {
        assertEquals(CoverSeed.hash("Kid A"), CoverSeed.hash("  kid a "))
        assertEquals(
            CoverSeed.of("Kid A", CoverStyle.DOTS, 1),
            CoverSeed.of("kid a", CoverStyle.DOTS, 1),
        )
    }

    @Test
    fun `variant, style and album each change the seed`() {
        val base = CoverSeed.of("Blue", CoverStyle.SHAPES, 0)
        assertNotEquals(base, CoverSeed.of("Blue", CoverStyle.SHAPES, 1))
        assertNotEquals(base, CoverSeed.of("Blue", CoverStyle.STRIPES, 0))
        assertNotEquals(base, CoverSeed.of("Court and Spark", CoverStyle.SHAPES, 0))
    }

    @Test
    fun `shuffle gives a different design`() {
        val request = CoverRequest(key = "Blue", style = CoverStyle.SHAPES)
        assertNotEquals(CoverGenerator.design(request), CoverGenerator.design(request.copy(variant = 1)))
    }

    @Test
    fun `mixed style is stable per album`() {
        val a = CoverGenerator.design(CoverRequest(key = "Vespertine", style = null, variant = 2))
        val b = CoverGenerator.design(CoverRequest(key = "Vespertine", style = null, variant = 2))
        assertEquals(a.style, b.style)
        assertEquals(a, b)
        assertEquals(CoverSeed.mixedStyle("Vespertine", 2), a.style)
    }

    @Test
    fun `seeded random repeats`() {
        val a = SeededRandom(42L)
        val b = SeededRandom(42L)
        repeat(100) { assertEquals(a.nextLong(), b.nextLong()) }
        val r = SeededRandom(7L)
        repeat(1000) {
            val f = r.nextFloat()
            assertTrue(f >= 0f && f < 1f)
            val i = r.nextInt(5)
            assertTrue(i in 0..4)
        }
    }

    @Test
    fun `cover uses at most three colours`() {
        CoverStyle.entries.forEach { style ->
            val design = CoverGenerator.design(CoverRequest(key = "Homogenic", style = style, palette = CoverPaletteChoice.EMBER))
            val used = design.shapes.map { it.color }.toSet() + design.palette.background
            assertTrue(used.size <= 3, "style $style used ${used.size} colours")
            assertTrue(used.all { it in design.palette.colors })
        }
    }

    @Test
    fun `text toggle and placement`() {
        val withText = CoverGenerator.design(CoverRequest(key = "Debut", style = CoverStyle.DOTS, showText = true))
        val noText = CoverGenerator.design(CoverRequest(key = "Debut", style = CoverStyle.DOTS, showText = false))
        assertNotNull(withText.text)
        assertNull(noText.text)
        val text = withText.text!!
        assertEquals("Debut", text.text)
        // Pattern stays above the title so the title always sits on plain background.
        assertTrue(withText.frame.bottom <= text.bottom - text.maxHeight)
    }

    @Test
    fun `text colour follows the cover background`() {
        val design = CoverGenerator.design(CoverRequest(key = "Ok", style = CoverStyle.WAVES, palette = CoverPaletteChoice.INK))
        assertEquals(YuyuCoverColors.PAPER, design.text!!.color)
        val light = CoverGenerator.design(CoverRequest(key = "Ok", style = CoverStyle.WAVES, palette = CoverPaletteChoice.PEACH))
        assertEquals(YuyuCoverColors.INK, light.text!!.color)
    }
}

class CoverPaletteContrastTest {

    @Test
    fun `ink on light backgrounds, paper on dark ones`() {
        assertEquals(YuyuCoverColors.PAPER, CoverContrast.textColorOn(YuyuCoverColors.INK))
        assertEquals(YuyuCoverColors.INK, CoverContrast.textColorOn(YuyuCoverColors.PAPER))
        assertEquals(YuyuCoverColors.INK, CoverContrast.textColorOn(YuyuCoverColors.PAPER_WARM))
        assertEquals(YuyuCoverColors.INK, CoverContrast.textColorOn(YuyuCoverColors.LAVENDER))
        assertEquals(YuyuCoverColors.INK, CoverContrast.textColorOn(YuyuCoverColors.MINT))
        assertEquals(YuyuCoverColors.INK, CoverContrast.textColorOn(YuyuCoverColors.PEACH))
        assertEquals(YuyuCoverColors.PAPER, CoverContrast.textColorOn(YuyuCoverColors.EMBER))
    }

    @Test
    fun `picked text colour is never the weaker choice`() {
        val backgrounds = YuyuCoverColors.ART_PALETTE + listOf(
            YuyuCoverColors.INK, YuyuCoverColors.PAPER, YuyuCoverColors.EMBER, YuyuCoverColors.LAVENDER,
            YuyuCoverColors.MINT, YuyuCoverColors.PEACH, YuyuCoverColors.HIGHLIGHT_MINT, YuyuCoverColors.HIGHLIGHT_LAVENDER,
        )
        backgrounds.forEach { bg ->
            val picked = CoverContrast.textColorOn(bg)
            val other = if (picked == YuyuCoverColors.INK) YuyuCoverColors.PAPER else YuyuCoverColors.INK
            assertTrue(CoverContrast.ratio(picked, bg) >= CoverContrast.ratio(other, bg))
            assertTrue(CoverContrast.ratio(picked, bg) >= 3.0, "bg ${Integer.toHexString(bg)} too low")
        }
    }

    @Test
    fun `contrast ratio of black on white is 21`() {
        assertEquals(21.0, CoverContrast.ratio(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 0.01)
        assertEquals(1.0, CoverContrast.ratio(YuyuCoverColors.EMBER, YuyuCoverColors.EMBER), 0.0001)
    }

    @Test
    fun `cover palette matches the YuyuArt placeholder colour`() {
        val key = "Fetch the Bolt Cutters"
        val palette = CoverPalettes.resolve(CoverPaletteChoice.COVER, key)
        assertEquals(YuyuCoverColors.ART_PALETTE[(key.hashCode() and 0x7fffffff) % 8], palette.background)
    }

    @Test
    fun `auto palette is stable for an album`() {
        val a = CoverPalettes.resolve(CoverPaletteChoice.AUTO, "Blonde", SeededRandom(5L))
        val b = CoverPalettes.resolve(CoverPaletteChoice.AUTO, "Blonde", SeededRandom(5L))
        assertEquals(a, b)
    }
}
