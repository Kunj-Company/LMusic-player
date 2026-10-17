package com.twilighty.lmusic.presentation.yuyu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twilighty.lmusic.R
import com.twilighty.lmusic.data.model.Song
import com.twilighty.lmusic.data.stats.PlaybackStatsRepository
import com.twilighty.lmusic.utils.formatListeningDurationCompact
import java.util.Calendar

private fun greetingForNow(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Late night"
    }
}

/** Home's header: kicker greeting, "Listen", and the quick actions. */
@Composable
fun YuyuHomeHeader(
    onSettingsClick: () -> Unit,
    onWhatsNewClick: () -> Unit,
    onSourcesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val greeting = remember { greetingForNow() }
    YuyuTopBar(
        title = "Listen",
        kicker = greeting,
        modifier = modifier.statusBarsPadding().padding(top = 8.dp),
        actions = {
            YuyuIconButton(
                painter = painterResource(R.drawable.round_newspaper_24),
                contentDescription = "What's new",
                onClick = onWhatsNewClick
            )
            YuyuIconButton(
                imageVector = Icons.Rounded.Cloud,
                contentDescription = "Music sources",
                onClick = onSourcesClick
            )
            YuyuIconButton(
                painter = painterResource(R.drawable.rounded_settings_24),
                contentDescription = "Settings",
                onClick = onSettingsClick,
                style = YuyuIconButtonStyle.Tonal
            )
        }
    )
}

/** The accent "Your mix" tile with a cover strip and an inverse play button. */
@Composable
fun YuyuMixTile(
    songs: List<Song>,
    isShuffleEnabled: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = Yuyu.accent
    val onAccent = Yuyu.onAccent
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(YuyuShapes.xxl)
            .background(accent)
            .clickable(onClick = onPlay)
            .padding(20.dp)
    ) {
        Text("TODAY", style = YuyuType.overline, color = onAccent.copy(alpha = 0.78f))
        Spacer(Modifier.height(4.dp))
        Text("Your mix", style = YuyuType.display(36.sp), color = onAccent)
        Spacer(Modifier.height(4.dp))
        val artists = remember(songs) {
            songs.asSequence().map { it.displayArtist }.filter { it.isNotBlank() }.distinct().take(3).toList()
        }
        Text(
            text = if (artists.isEmpty()) "${songs.size} songs from your library"
            else "${artists.joinToString(", ")} and more · ${songs.size} songs",
            style = YuyuType.meta,
            color = onAccent.copy(alpha = 0.86f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(56.dp)) {
                songs.take(4).forEachIndexed { index, song ->
                    YuyuArt(
                        model = song.albumArtUriString,
                        title = song.album.ifBlank { song.title },
                        size = 52.dp,
                        shape = YuyuShapes.md,
                        modifier = Modifier
                            .offset(x = (index * 38).dp)
                            .border(2.dp, accent, YuyuShapes.md)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(onAccent)
                    .clickable(role = Role.Button, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isShuffleEnabled) Icons.Rounded.Shuffle else Icons.Rounded.PlayArrow,
                    contentDescription = "Play your mix",
                    tint = accent,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

/** Horizontal row of recently played covers (148dp cards). */
@Composable
fun YuyuRecentlyPlayedRow(
    songs: List<Song>,
    currentSongId: String?,
    onSongClick: (Song) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        YuyuSectionHeader(
            title = "Recently played",
            actionLabel = "See all",
            onAction = onSeeAll,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs, key = { "recent_${it.id}" }) { song ->
                Column(
                    modifier = Modifier
                        .width(148.dp)
                        .clip(YuyuShapes.lg)
                        .clickable { onSongClick(song) }
                ) {
                    YuyuArt(
                        model = song.albumArtUriString,
                        title = song.album.ifBlank { song.title },
                        size = 148.dp,
                        shape = YuyuShapes.lg
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        song.title,
                        style = YuyuType.title.copy(fontSize = YuyuType.label.fontSize),
                        color = if (song.id == currentSongId) Yuyu.accentText else Yuyu.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.displayArtist,
                        style = YuyuType.caption,
                        color = Yuyu.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** "This week" — listening time, plays and the top artist as Yuyu stat tiles. */
@Composable
fun YuyuWeekStats(
    summary: PlaybackStatsRepository.PlaybackStatsSummary,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 16.dp)) {
        val title = when (summary.range) {
            com.twilighty.lmusic.data.stats.StatsTimeRange.DAY -> "Today"
            com.twilighty.lmusic.data.stats.StatsTimeRange.WEEK -> "This week"
            com.twilighty.lmusic.data.stats.StatsTimeRange.MONTH -> "This month"
            com.twilighty.lmusic.data.stats.StatsTimeRange.YEAR -> "This year"
            else -> "Your listening"
        }
        YuyuSectionHeader(title = title, actionLabel = "Stats", onAction = onOpenStats)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            YuyuStatTile(
                label = "Listening",
                value = formatListeningDurationCompact(summary.totalDurationMs),
                supporting = "${summary.activeDays} active days",
                modifier = Modifier.weight(1f),
                onClick = onOpenStats
            )
            YuyuStatTile(
                label = "Plays",
                value = summary.totalPlayCount.toString(),
                supporting = "${summary.uniqueSongs} songs",
                modifier = Modifier.weight(1f),
                onClick = onOpenStats
            )
        }
        val topArtist = summary.topArtists.firstOrNull()
        val topSong = summary.topSongs.firstOrNull()
        if (topArtist != null || topSong != null) {
            Spacer(Modifier.height(12.dp))
            YuyuCard(onClick = onOpenStats, modifier = Modifier.fillMaxWidth()) {
                if (topArtist != null) {
                    YuyuOverline("Top artist")
                    Text(topArtist.artist, style = YuyuType.h3, color = Yuyu.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${topArtist.playCount} plays", style = YuyuType.meta, color = Yuyu.textMuted)
                }
                if (topArtist != null && topSong != null) {
                    Spacer(Modifier.height(12.dp))
                    YuyuDivider()
                    Spacer(Modifier.height(12.dp))
                }
                if (topSong != null) {
                    YuyuOverline("On repeat")
                    Text(topSong.title, style = YuyuType.title, color = Yuyu.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${topSong.artist} · ${topSong.playCount} plays", style = YuyuType.meta, color = Yuyu.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

