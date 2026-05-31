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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.tabs.SecondaryTabRowMMD
import com.mudita.mmd.components.tabs.TabMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.player.mindful.model.RepeatMode
import com.player.mindful.model.SortMode
import com.player.mindful.ui.theme.MudBlack
import com.player.mindful.viewmodel.PlayerViewModel

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
        modifier = Modifier.height(32.dp),
        shape = RectangleShape,
        border = BorderStroke(1.dp, MudBlack),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    ) {
        TextMMD("SKIN $index ▶", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MudBlack)
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextMMD(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = MudBlack)
            TextMMD(if (expanded) "−" else "+", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MudBlack)
        }
        if (expanded) {
            HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            Box(modifier = Modifier.padding(10.dp)) { content() }
        }
    }
}

@Composable
internal fun MultiSelector(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    fontSize: TextUnit = 9.sp,
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
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                if (fontFamily != null)
                    TextMMD(opt, fontSize = fontSize, fontFamily = fontFamily, fontWeight = FontWeight.Bold, color = if (i == selected) Color.White else MudBlack)
                else
                    TextMMD(opt, fontSize = fontSize, fontWeight = FontWeight.Bold, color = if (i == selected) Color.White else MudBlack)
            }
        }
    }
}

@Composable
internal fun PhysicalSwitch(isOn: Boolean, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
    ) {
        SwitchMMD(checked = isOn, onCheckedChange = { onClick() })
        if (label.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            TextMMD(label, fontSize = 8.sp, letterSpacing = 1.sp, color = MudBlack)
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
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp)
        ) {
            TextMMD(modeText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (repeatMode != RepeatMode.OFF) Color.White else MudBlack)
        }
        if (label.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            TextMMD(label, fontSize = 8.sp, letterSpacing = 1.sp, color = MudBlack)
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
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextFieldMMD(
                value = searchText,
                onValueChange = onSearch,
                modifier = Modifier.weight(1f),
                placeholder = { TextMMD("SEARCH TRACKS...", fontSize = 10.sp, color = MudBlack.copy(alpha = 0.4f)) },
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
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(40.dp)
            ) { TextMMD(sortLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            SecondaryTabRowMMD(selectedTabIndex = libraryTab, modifier = Modifier.weight(1f)) {
                listOf("ALL", "ALBUMS", "ARTISTS").forEachIndexed { i, title ->
                    TabMMD(selected = i == libraryTab, onClick = { onTab(i) }, text = { TextMMD(title, fontSize = 9.sp, fontWeight = FontWeight.Bold) })
                }
            }
            TextMMD("$trackCount", fontSize = 9.sp, color = MudBlack.copy(alpha = 0.6f), modifier = Modifier.padding(start = 8.dp))
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
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, MudBlack),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) { TextMMD(lbl, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
            }
    }
}

@Composable
internal fun BoxScope.MudScrollbar(listState: LazyListState) {
    val total   = listState.layoutInfo.totalItemsCount
    val visible = listState.layoutInfo.visibleItemsInfo.size
    if (total <= visible) return
    val thumbFrac   = visible.toFloat() / total
    val thumbOffset = listState.firstVisibleItemIndex.toFloat() / total
    Canvas(modifier = Modifier.width(4.dp).fillMaxHeight().align(Alignment.CenterEnd)) {
        drawRect(Color.Black.copy(alpha = 0.1f), size = size)
        drawRect(Color.Black, topLeft = Offset(0f, size.height * thumbOffset), size = Size(size.width, size.height * thumbFrac))
    }
}
