package com.twilighty.lmusic.presentation.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twilighty.lmusic.data.update.UpdateNotifier
import com.twilighty.lmusic.presentation.yuyu.Yuyu
import com.twilighty.lmusic.presentation.yuyu.YuyuButton
import com.twilighty.lmusic.presentation.yuyu.YuyuButtonSize
import com.twilighty.lmusic.presentation.yuyu.YuyuButtonStyle
import com.twilighty.lmusic.presentation.yuyu.YuyuIconButton
import com.twilighty.lmusic.presentation.yuyu.YuyuShapes
import com.twilighty.lmusic.presentation.yuyu.YuyuType

/** Small dismissible "Update available" banner for Home. Renders nothing when there's no update. */
@Composable
fun UpdateBanner(
    modifier: Modifier = Modifier,
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.showBanner) return
    val version = state.availableVersion ?: return
    val url = state.url ?: return
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Yuyu.accentSoft, YuyuShapes.lg)
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = Yuyu.accentText, modifier = Modifier.size(20.dp))
        Text(
            "Update available · LMusic $version",
            style = YuyuType.label,
            color = Yuyu.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        YuyuButton(
            text = "Open",
            onClick = { runCatching { context.startActivity(UpdateNotifier.openReleaseIntent(url)) } },
            style = YuyuButtonStyle.Primary,
            size = YuyuButtonSize.Small
        )
        YuyuIconButton(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Dismiss",
            onClick = { viewModel.dismissBanner(version) },
            size = 36.dp,
            iconSize = 18.dp
        )
    }
}

/** True when Home should show [UpdateBanner] (lets Home skip the list item entirely). */
@Composable
fun rememberUpdateBannerVisible(viewModel: UpdateViewModel = hiltViewModel()): Boolean {
    val state by viewModel.state.collectAsStateWithLifecycle()
    return state.showBanner && state.url != null
}
