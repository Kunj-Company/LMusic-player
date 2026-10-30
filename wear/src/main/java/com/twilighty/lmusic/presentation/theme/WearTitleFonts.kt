package com.twilighty.lmusic.presentation.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.twilighty.lmusic.R

@OptIn(ExperimentalTextApi::class)
val WearTitleFont = FontFamily(
    Font(
        resId = R.font.gflex_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(134f),
            FontVariation.Setting("ROND", 74f),
            FontVariation.Setting("XTRA", 540f),
            FontVariation.Setting("YOPQ", 92f),
            FontVariation.Setting("YTLC", 510f),
        ),
    ),
)

fun rememberBrowseSubscreenTitleFont(): FontFamily = WearTitleFont

/** The Library screen title. */
@OptIn(ExperimentalTextApi::class)
val WearLibraryTitleFont = FontFamily(
    Font(
        resId = R.font.gflex_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(650),
            FontVariation.width(146f),
            FontVariation.Setting("ROND", 56f),
            FontVariation.Setting("XTRA", 520f),
            FontVariation.Setting("YOPQ", 90f),
            FontVariation.Setting("YTLC", 505f),
        ),
    ),
)

/** The On Watch screen title. */
@OptIn(ExperimentalTextApi::class)
val WearWatchLibraryTitleFont = FontFamily(
    Font(
        resId = R.font.gflex_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(710),
            FontVariation.width(132f),
            FontVariation.Setting("ROND", 82f),
            FontVariation.Setting("XTRA", 540f),
            FontVariation.Setting("YOPQ", 92f),
            FontVariation.Setting("YTLC", 512f),
        ),
    ),
)
