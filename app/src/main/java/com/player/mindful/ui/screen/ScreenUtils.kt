package com.player.mindful.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.tabs.SecondaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.player.mindful.model.Playlist
import com.player.mindful.model.RepeatMode
import com.player.mindful.model.SortMode
import com.player.mindful.model.Track
import com.player.mindful.ui.theme.MudBlack
import com.player.mindful.viewmodel.PlayerViewModel
import kotlin.math.ceil

private val MIN_TOUCH = 44.dp

internal val ROW_VPAD = 12.dp

internal fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
internal fun ThemeSwitchButton(viewModel: PlayerViewModel) {
    val state by viewModel.state.collectAsState()
    val index = when (state.theme) {
        com.player.mindful.model.PlayerTheme.MARSHALL -> "1/3"
        com.player.mindful.model.PlayerTheme.SYNTH    -> "2/3"
        com.player.mindful.model.PlayerTheme.SOVIET   -> "3/3"
    }
    OutlinedButtonMMD(
        onClick = { viewModel.cycleTheme() },
        modifier = Modifier.heightIn(min = MIN_TOUCH),
        shape = RectangleShape,
        border = BorderStroke(1.dp, MudBlack),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        TextMMD("SKIN $index ▶", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MudBlack)
    }
}

@Composable
internal fun CollapsiblePanel(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth().border(1.dp, MudBlack)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onToggle() }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextMMD(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = MudBlack)
            TextMMD(if (expanded) "−" else "+", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MudBlack)
        }
        if (expanded) {
            HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            Box(modifier = Modifier.padding(12.dp)) { content() }
        }
    }
}

@Composable
internal fun MultiSelector(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    fontSize: TextUnit = 11.sp,
    fontFamily: FontFamily? = null
) {
    Row {
        options.forEachIndexed { i, opt ->
            OutlinedButtonMMD(
                onClick = { onSelect(i) },
                shape = RectangleShape,
                border = BorderStroke(1.dp, MudBlack),
                colors = if (i == selected)
                    ButtonDefaults.outlinedButtonColors(containerColor = MudBlack, contentColor = Color.White)
                else
                    ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = MudBlack),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.heightIn(min = MIN_TOUCH)
            ) {
                if (fontFamily != null)
                    TextMMD(opt, fontSize = fontSize, fontFamily = fontFamily, fontWeight = FontWeight.Bold, color = if (i == selected) Color.White else MudBlack)
                else
                    TextMMD(opt, fontSize = fontSize, fontWeight = FontWeight.Bold, color = if (i == selected) Color.White else MudBlack)
            }
        }
    }
}

internal enum class SwitchStyle { AMP_FLIP, LEVER, LOCK_LEVER }
internal enum class LedShape { ROUND, SQUARE }

@Composable
internal fun LedIndicator(on: Boolean, shape: LedShape = LedShape.ROUND, dim: androidx.compose.ui.unit.Dp = 11.dp) {
    Canvas(modifier = Modifier.size(dim)) {
        when (shape) {
            LedShape.ROUND -> {
                val r = this.size.minDimension / 2
                drawCircle(MudBlack, radius = r, style = Stroke(1.5.dp.toPx()))
                if (on) drawCircle(MudBlack, radius = r - 2.5.dp.toPx())
            }
            LedShape.SQUARE -> {
                drawRect(MudBlack, style = Stroke(1.5.dp.toPx()))
                if (on) drawRect(
                    MudBlack,
                    topLeft = Offset(2.5.dp.toPx(), 2.5.dp.toPx()),
                    size = Size(this.size.width - 5.dp.toPx(), this.size.height - 5.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun AmpFlipGraphic(isOn: Boolean) {
    Canvas(modifier = Modifier.size(width = 52.dp, height = 28.dp)) {
        drawRoundRect(MudBlack, size = Size(size.width, size.height), cornerRadius = CornerRadius(size.height / 2), style = Stroke(2.dp.toPx()))
        val paddleW = size.width * 0.46f
        val paddleX = if (isOn) size.width - paddleW - 3.dp.toPx() else 3.dp.toPx()
        drawRoundRect(
            MudBlack, topLeft = Offset(paddleX, 3.dp.toPx()),
            size = Size(paddleW, size.height - 6.dp.toPx()),
            cornerRadius = CornerRadius((size.height - 6.dp.toPx()) / 2)
        )
    }
}

@Composable
private fun LeverGraphic(isOn: Boolean) {
    Canvas(modifier = Modifier.size(width = 26.dp, height = 50.dp)) {
        val inset = 5.dp.toPx()
        drawRoundRect(
            MudBlack, topLeft = Offset(size.width / 2 - 4.dp.toPx(), inset),
            size = Size(8.dp.toPx(), size.height - inset * 2), style = Stroke(2.dp.toPx())
        )
        val leverY = if (isOn) inset + 7.dp.toPx() else size.height - inset - 7.dp.toPx()
        drawLine(MudBlack, Offset(2.dp.toPx(), leverY), Offset(size.width - 2.dp.toPx(), leverY), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(MudBlack, radius = 3.5.dp.toPx(), center = Offset(size.width / 2, leverY))
    }
}

@Composable
private fun LockLeverGraphic(isOn: Boolean) {
    Canvas(modifier = Modifier.size(width = 30.dp, height = 50.dp)) {
        val inset = 5.dp.toPx()
        drawLine(MudBlack, Offset(3.dp.toPx(), inset), Offset(3.dp.toPx(), size.height - inset), strokeWidth = 2.5.dp.toPx())
        drawLine(MudBlack, Offset(size.width - 3.dp.toPx(), inset), Offset(size.width - 3.dp.toPx(), size.height - inset), strokeWidth = 2.5.dp.toPx())
        drawLine(MudBlack, Offset(3.dp.toPx(), size.height - inset), Offset(size.width - 3.dp.toPx(), size.height - inset), strokeWidth = 2.5.dp.toPx())
        drawLine(MudBlack, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2.dp.toPx())
        val leverY = if (isOn) inset + 9.dp.toPx() else size.height - inset - 9.dp.toPx()
        drawLine(MudBlack, Offset(size.width / 2, size.height / 2), Offset(size.width / 2, leverY), strokeWidth = 3.5.dp.toPx())
        drawCircle(MudBlack, radius = 6.dp.toPx(), center = Offset(size.width / 2, leverY))
    }
}

@Composable
internal fun StyledSwitch(isOn: Boolean, label: String, style: SwitchStyle, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .defaultMinSize(minWidth = MIN_TOUCH, minHeight = MIN_TOUCH)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
            .padding(4.dp)
    ) {
        when (style) {
            SwitchStyle.AMP_FLIP -> AmpFlipGraphic(isOn)
            SwitchStyle.LEVER -> LeverGraphic(isOn)
            SwitchStyle.LOCK_LEVER -> LockLeverGraphic(isOn)
        }
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            LedIndicator(on = isOn, shape = if (style == SwitchStyle.AMP_FLIP) LedShape.ROUND else LedShape.SQUARE)
            if (label.isNotEmpty()) {
                TextMMD(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MudBlack)
            }
        }
    }
}

@Composable
internal fun RepeatButton(repeatMode: RepeatMode, label: String, onClick: () -> Unit) {
    val modeText = when (repeatMode) { RepeatMode.OFF -> "OFF"; RepeatMode.ALL -> "ALL"; RepeatMode.ONE -> " 1 " }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
    ) {
        OutlinedButtonMMD(
            onClick = onClick,
            shape = RectangleShape,
            border = BorderStroke(2.dp, MudBlack),
            colors = if (repeatMode != RepeatMode.OFF)
                ButtonDefaults.outlinedButtonColors(containerColor = MudBlack, contentColor = Color.White)
            else
                ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = MudBlack),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.heightIn(min = MIN_TOUCH)
        ) {
            TextMMD(modeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (repeatMode != RepeatMode.OFF) Color.White else MudBlack)
        }
        if (label.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            TextMMD(label, fontSize = 10.sp, letterSpacing = 1.sp, color = MudBlack)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryControls(
    searchText: String,
    sortMode: SortMode,
    libraryTab: Int,
    trackCount: Int,
    onSearch: (String) -> Unit,
    onSort: () -> Unit,
    onTab: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextFieldMMD(
                value = searchText,
                onValueChange = onSearch,
                modifier = Modifier.weight(1f),
                placeholder = { TextMMD("SEARCH TRACKS...", fontSize = 12.sp, color = MudBlack) },
                singleLine = true
            )
            Spacer(Modifier.width(6.dp))
            val sortLabel = when (sortMode) {
                SortMode.TITLE_ASC -> "A-Z"; SortMode.TITLE_DESC -> "Z-A"
                SortMode.ARTIST -> "ART"; SortMode.DURATION -> "DUR"
            }
            OutlinedButtonMMD(
                onClick = onSort,
                shape = RectangleShape,
                border = BorderStroke(1.dp, MudBlack),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.heightIn(min = MIN_TOUCH)
            ) { TextMMD(sortLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            SecondaryTabRowMMD(selectedTabIndex = libraryTab, modifier = Modifier.weight(1f)) {
                listOf("ALL", "ALBUMS", "ARTISTS", "LISTS").forEachIndexed { i, title ->
                    TabMMD(selected = i == libraryTab, onClick = { onTab(i) }, text = { TextMMD(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                }
            }
            TextMMD("$trackCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MudBlack, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
internal fun SleepTimerControl(remainingSec: Int?, onSet: (Int?) -> Unit) {
    var idx by remember { mutableStateOf(0) }
    val labels  = listOf("OFF", "15m", "30m", "45m", "60m")
    val minutes = listOf(null, 15, 30, 45, 60)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD("SLEEP", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = MudBlack)
            Spacer(Modifier.width(8.dp))
            MultiSelector(labels, idx, { idx = it; onSet(minutes[it]) }, 10.sp)
        }
        if (remainingSec != null) {
            Spacer(Modifier.height(6.dp))
            TextMMD("STOPS IN ${formatMs(remainingSec * 1000L)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MudBlack)
        }
    }
}

@Composable
internal fun SleepBatteryControl(remainingSec: Int?, totalSec: Int?, onSet: (Int?) -> Unit) {
    val presets = listOf(null, 15, 30, 45, 60)
    var idx by remember { mutableStateOf(0) }
    val isActive = remainingSec != null && totalSec != null && totalSec > 0
    val segments = if (isActive) ceil(remainingSec!!.toFloat() / totalSec!! * 5f).toInt().coerceIn(1, 5) else 5

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .heightIn(min = MIN_TOUCH)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                idx = (idx + 1) % presets.size
                onSet(presets[idx])
            }
    ) {
        TextMMD("SLEEP", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = MudBlack)
        BatteryGraphic(segments = segments, filled = isActive)
        TextMMD(
            if (isActive) "${formatMs(remainingSec!! * 1000L)} LEFT" else "TAP TO SET TIMER",
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MudBlack
        )
    }
}

@Composable
private fun BatteryGraphic(segments: Int, filled: Boolean) {
    Canvas(modifier = Modifier.size(width = 40.dp, height = 20.dp)) {
        val bodyW = size.width - 5.dp.toPx()
        drawRoundRect(MudBlack, size = Size(bodyW, size.height), cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(2.dp.toPx()))
        drawRect(MudBlack, topLeft = Offset(bodyW + 1.dp.toPx(), size.height * 0.28f), size = Size(4.dp.toPx(), size.height * 0.44f))
        val pad = 4.dp.toPx(); val gap = 2.dp.toPx()
        val segW = (bodyW - pad * 2 - gap * 4) / 5
        repeat(5) { i ->
            if (filled && i < segments) {
                drawRect(MudBlack, topLeft = Offset(pad + i * (segW + gap), pad), size = Size(segW, size.height - pad * 2))
            }
        }
    }
}

@Composable
internal fun DottedDivider() {
    Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            MudBlack, start = Offset(0f, 0f), end = Offset(size.width, 0f),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
        )
    }
}

@Composable
internal fun QueueItemRow(track: Track, onPlay: () -> Unit, onRemove: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()
        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onPlay)
        .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TextMMD(track.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MudBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextMMD(track.artist, fontSize = 12.sp, color = MudBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier.size(MIN_TOUCH)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onRemove),
            contentAlignment = Alignment.Center
        ) { TextMMD("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
    }
}

@Composable
internal fun PlaylistRow(playlist: Playlist, onPlay: () -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()
        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onPlay)
        .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TextMMD(playlist.name.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MudBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextMMD("${playlist.trackIds.size} TRACKS", fontSize = 12.sp, color = MudBlack)
        }
        Box(
            modifier = Modifier.size(MIN_TOUCH)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onDelete),
            contentAlignment = Alignment.Center
        ) { TextMMD("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
    }
}

@Composable
internal fun NewPlaylistRow(onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextFieldMMD(
            value = name, onValueChange = { name = it }, modifier = Modifier.weight(1f),
            placeholder = { TextMMD("NEW PLAYLIST NAME...", fontSize = 12.sp, color = MudBlack) },
            singleLine = true
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier.border(1.dp, MudBlack).heightIn(min = MIN_TOUCH)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    if (name.isNotBlank()) { onCreate(name); name = "" }
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) { TextMMD("+ CREATE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackActionsSheet(
    track: Track,
    playlists: List<Playlist>,
    onPlayNext: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onCreatePlaylistWithTrack: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    ModalBottomSheetMMD(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            TextMMD(track.title.uppercase(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MudBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH).border(1.dp, MudBlack)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onPlayNext(); onDismiss() }
                    .padding(12.dp),
                contentAlignment = Alignment.CenterStart
            ) { TextMMD("▶  PLAY NEXT", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
            Spacer(Modifier.height(14.dp))
            TextMMD("ADD TO PLAYLIST", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MudBlack)
            Spacer(Modifier.height(8.dp))
            playlists.forEach { pl ->
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH).border(1.dp, MudBlack)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onAddToPlaylist(pl.id); onDismiss() }
                        .padding(10.dp),
                    contentAlignment = Alignment.CenterStart
                ) { TextMMD(pl.name, fontSize = 13.sp, color = MudBlack, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextFieldMMD(
                    value = newName, onValueChange = { newName = it }, modifier = Modifier.weight(1f),
                    placeholder = { TextMMD("NEW PLAYLIST...", fontSize = 12.sp, color = MudBlack) },
                    singleLine = true
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier.border(1.dp, MudBlack).size(MIN_TOUCH)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            if (newName.isNotBlank()) { onCreatePlaylistWithTrack(newName); newName = ""; onDismiss() }
                        },
                    contentAlignment = Alignment.Center
                ) { TextMMD("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = MIN_TOUCH).border(2.dp, MudBlack)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) { TextMMD("CLOSE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
        }
    }
}

@Composable
internal fun TransportButtonRow(
    isPlaying: Boolean,
    onPrev: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit, onStop: () -> Unit,
    labelPrev: String = "◀◀", labelPlay: String = if (isPlaying) "▐▐" else "▶",
    labelNext: String = "▶▶", labelStop: String = "■",
    fontSize: Int = 15
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf(labelPrev to onPrev, labelPlay to onPlayPause, labelNext to onNext, labelStop to onStop)
            .forEach { (lbl, act) ->
                OutlinedButtonMMD(
                    onClick = act,
                    modifier = Modifier.weight(1f).heightIn(min = MIN_TOUCH),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, MudBlack),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) { TextMMD(lbl, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
            }
    }
}

@Composable
internal fun BoxScope.MudScrollbar(listState: LazyListState) {
    val layoutInfo by remember { derivedStateOf { listState.layoutInfo } }
    val showScrollbar by remember { derivedStateOf { layoutInfo.totalItemsCount > layoutInfo.visibleItemsInfo.size } }
    if (!showScrollbar) return

    val thumbFrac by remember {
        derivedStateOf {
            (layoutInfo.visibleItemsInfo.size.toFloat() / layoutInfo.totalItemsCount).coerceIn(0.08f, 0.92f)
        }
    }
    val thumbOffset by remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex.toFloat() / layoutInfo.totalItemsCount).coerceIn(0f, 1f - thumbFrac)
        }
    }

    Canvas(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 6.dp, top = 4.dp, bottom = 4.dp)
            .width(8.dp)
            .fillMaxHeight()
    ) {
        val r = CornerRadius(size.width / 2, size.width / 2)
        drawRoundRect(Color.Black, size = size, cornerRadius = r, style = Stroke(1.5.dp.toPx()))
        drawRoundRect(
            Color.Black,
            topLeft = Offset(0f, size.height * thumbOffset),
            size = Size(size.width, size.height * thumbFrac),
            cornerRadius = r
        )
    }
}
