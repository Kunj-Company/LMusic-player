package com.twilighty.lmusic.data.covers

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Yuyu colours as ARGB ints (pure Kotlin so the generator and its tests need no Android). */
object YuyuCoverColors {
    const val INK: Int = 0xFF18181B.toInt()
    const val PAPER: Int = 0xFFFFFFFF.toInt()
    const val PAPER_WARM: Int = 0xFFFAFAF9.toInt()
    const val EMBER: Int = 0xFFC2410C.toInt()
    const val LAVENDER: Int = 0xFFB9AEFF.toInt()
    const val MINT: Int = 0xFF7EE8C4.toInt()
    const val PEACH: Int = 0xFFFDE7C8.toInt()
    const val HIGHLIGHT_MINT: Int = 0xFFBDF3DF.toInt()
    const val HIGHLIGHT_LAVENDER: Int = 0xFFF1EFFF.toInt()
    const val HIGHLIGHT_PEACH: Int = 0xFFFDE7C8.toInt()

    /** Same values and order as the YuyuArt placeholder palette (YuyuComponents.kt ArtPalette). */
    val ART_PALETTE: List<Int> = listOf(
        0xFFC2410C, 0xFF4B3FA8, 0xFF0F766E, 0xFF27272A,
        0xFF4D6B2F, 0xFF7E2F5E, 0xFF1D4E89, 0xFF8A4B2A,
    ).map { it.toInt() }

    /** The colour YuyuArt uses as the placeholder for [key], so a made cover matches what was shown. */
    fun artColorFor(key: String): Int = ART_PALETTE[(key.hashCode() and 0x7fffffff) % ART_PALETTE.size]
}

/** Three colours per cover: a background and two pattern colours. */
data class ResolvedPalette(val background: Int, val primary: Int, val secondary: Int) {
    val colors: List<Int> get() = listOf(background, primary, secondary)
}

/** A palette chip in the studio. [AUTO] picks one per album from the seed. */
enum class CoverPaletteChoice(val label: String) {
    AUTO("Auto"),
    EMBER("Ember"),
    INK("Ink"),
    LAVENDER("Lavender"),
    MINT("Mint"),
    PEACH("Peach"),
    PAPER("Paper"),
    PASTEL("Pastel"),
    COVER("Cover");

    companion object {
        val fixed: List<CoverPaletteChoice> = entries.filter { it != AUTO }
    }
}

object CoverPalettes {
    private val C = YuyuCoverColors

    fun resolve(choice: CoverPaletteChoice, key: String, rng: SeededRandom? = null): ResolvedPalette = when (choice) {
        CoverPaletteChoice.AUTO -> {
            val pick = if (rng != null) CoverPaletteChoice.fixed[rng.nextInt(CoverPaletteChoice.fixed.size)]
            else CoverPaletteChoice.fixed[(key.hashCode() and 0x7fffffff) % CoverPaletteChoice.fixed.size]
            resolve(pick, key, null)
        }
        CoverPaletteChoice.EMBER -> ResolvedPalette(C.PAPER_WARM, C.EMBER, C.INK)
        CoverPaletteChoice.INK -> ResolvedPalette(C.INK, C.EMBER, C.PEACH)
        CoverPaletteChoice.LAVENDER -> ResolvedPalette(C.LAVENDER, C.INK, C.PAPER)
        CoverPaletteChoice.MINT -> ResolvedPalette(C.MINT, C.INK, C.PAPER_WARM)
        CoverPaletteChoice.PEACH -> ResolvedPalette(C.PEACH, C.EMBER, C.INK)
        CoverPaletteChoice.PAPER -> ResolvedPalette(C.PAPER, C.INK, C.LAVENDER)
        CoverPaletteChoice.PASTEL -> ResolvedPalette(C.HIGHLIGHT_MINT, C.LAVENDER, C.INK)
        CoverPaletteChoice.COVER -> {
            val bg = C.artColorFor(key)
            ResolvedPalette(bg, C.PEACH, C.PAPER)
        }
    }
}

/** WCAG contrast maths on ARGB ints. */
object CoverContrast {
    private fun channel(c: Int): Double {
        val s = c / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    fun luminance(argb: Int): Double {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    fun ratio(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** Ink or paper, whichever reads better on [background]. */
    fun textColorOn(background: Int): Int =
        if (ratio(YuyuCoverColors.INK, background) >= ratio(YuyuCoverColors.PAPER, background)) {
            YuyuCoverColors.INK
        } else {
            YuyuCoverColors.PAPER
        }
}
