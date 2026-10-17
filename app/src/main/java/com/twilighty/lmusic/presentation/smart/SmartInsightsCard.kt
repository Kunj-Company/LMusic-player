package com.twilighty.lmusic.presentation.smart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twilighty.lmusic.data.smart.SmartInsights
import com.twilighty.lmusic.data.stats.StatsTimeRange
import com.twilighty.lmusic.presentation.yuyu.Yuyu
import com.twilighty.lmusic.presentation.yuyu.YuyuCard
import com.twilighty.lmusic.presentation.yuyu.YuyuDivider
import com.twilighty.lmusic.presentation.yuyu.YuyuMeter
import com.twilighty.lmusic.presentation.yuyu.YuyuOverline
import com.twilighty.lmusic.presentation.yuyu.YuyuShapes
import com.twilighty.lmusic.presentation.yuyu.YuyuStatus
import com.twilighty.lmusic.presentation.yuyu.YuyuStatusChip
import com.twilighty.lmusic.presentation.yuyu.YuyuType
import kotlin.math.roundToInt

/**
 * Stats screen card with on-device smart insights for [range]. Shows nothing when smart
 * features are off. [refreshKey] reloads (e.g. the stats summary object).
 */
@Composable
fun SmartInsightsCard(
    range: StatsTimeRange,
    modifier: Modifier = Modifier,
    refreshKey: Any? = null,
    viewModel: SmartInsightsViewModel = hiltViewModel(),
) {
    val enabled by viewModel.isEnabled.collectAsStateWithLifecycle()
    val insights by viewModel.insights.collectAsStateWithLifecycle()
    LaunchedEffect(enabled, range, refreshKey) {
        if (enabled) viewModel.load(range, force = refreshKey != null)
    }
    if (!enabled) return
    SmartInsightsContent(insights = insights, modifier = modifier)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartInsightsContent(insights: SmartInsights?, modifier: Modifier = Modifier) {
    YuyuCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            YuyuOverline("Smart insights", modifier = Modifier.weight(1f))
            YuyuStatusChip(text = "Experimental", status = YuyuStatus.Neutral)
        }

        if (insights == null || insights.playCount == 0 || insights.moodMix.isEmpty()) {
            Text(
                if (insights == null) "Loading…" else "Play a few analysed songs to see your mood mix here.",
                style = YuyuType.body,
                color = Yuyu.textSecondary,
                modifier = Modifier.padding(top = 10.dp)
            )
            return@YuyuCard
        }

        // Taste drift
        Text(insights.tasteDriftLabel, style = YuyuType.h3, color = Yuyu.text, modifier = Modifier.padding(top = 10.dp))
        Text(
            buildString {
                append("Compared with the previous period")
                append(" · ")
                append("${(insights.discoveryRate * 100).roundToInt()}% of plays were new to you")
            },
            style = YuyuType.meta,
            color = Yuyu.textMuted
        )

        // Mood mix
        YuyuDivider(Modifier.padding(vertical = 14.dp))
        Text("Mood mix", style = YuyuType.title, color = Yuyu.text)
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            insights.moodMix.entries.take(5).forEachIndexed { index, (mood, share) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(mood, style = YuyuType.meta, color = Yuyu.text, modifier = Modifier.width(92.dp), maxLines = 1)
                    YuyuMeter(
                        progress = share.coerceIn(0f, 1f),
                        modifier = Modifier.weight(1f),
                        color = if (index == 0) Yuyu.accent else Yuyu.ink.copy(alpha = 0.55f)
                    )
                    Text(
                        "${(share * 100).roundToInt()}%",
                        style = YuyuType.caption,
                        color = Yuyu.textMuted,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        // Energy by hour
        YuyuDivider(Modifier.padding(vertical = 14.dp))
        Text("Energy by hour", style = YuyuType.title, color = Yuyu.text)
        EnergyByHourBars(insights.energyByHour, Modifier.padding(top = 10.dp))

        // Top genres
        if (insights.topGenres.isNotEmpty()) {
            YuyuDivider(Modifier.padding(vertical = 14.dp))
            Text("Top genres", style = YuyuType.title, color = Yuyu.text)
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                insights.topGenres.forEach { (genre, share) ->
                    Text(
                        "$genre ${(share * 100).roundToInt()}%",
                        style = YuyuType.caption,
                        color = Yuyu.text,
                        modifier = Modifier
                            .clip(YuyuShapes.pill)
                            .background(Yuyu.muted)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (insights.coverage < 0.6f) {
            Text(
                "Based on ${(insights.coverage * 100).roundToInt()}% of your plays. Indexing continues while charging.",
                style = YuyuType.caption,
                color = Yuyu.textMuted,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

@Composable
private fun EnergyByHourBars(values: List<Float?>, modifier: Modifier = Modifier) {
    val peak = values.filterNotNull().maxOrNull()?.coerceAtLeast(0.05f) ?: 1f
    val peakHour = values.indices.maxByOrNull { values[it] ?: -1f }
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            values.forEachIndexed { hour, v ->
                val fraction = if (v == null) 0.04f else (v / peak).coerceIn(0.08f, 1f)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .clip(YuyuShapes.sm)
                        .background(
                            when {
                                v == null -> Yuyu.emphasis
                                hour == peakHour -> Yuyu.accent
                                else -> Yuyu.ink.copy(alpha = 0.45f)
                            }
                        )
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            listOf("12a", "6a", "12p", "6p").forEach { label ->
                Text(label, style = YuyuType.caption, color = Yuyu.textMuted, modifier = Modifier.weight(1f))
            }
        }
    }
}
