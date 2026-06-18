package com.player.mindful.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.player.mindful.model.PlayerUiState
import com.player.mindful.model.Track
import com.player.mindful.ui.theme.MudBlack
import com.player.mindful.viewmodel.PlayerViewModel

@Composable
fun SovietScreen(viewModel: PlayerViewModel) {
    val state by viewModel.state.collectAsState()

    var vuOpen      by remember { mutableStateOf(true) }
    var nowPlayOpen by remember { mutableStateOf(true) }
    var ctrlOpen    by remember { mutableStateOf(true) }
    var volOpen     by remember { mutableStateOf(true) }
    var panelOpen   by remember { mutableStateOf(false) }
    var libraryOpen by remember { mutableStateOf(true) }
    var queueOpen   by remember { mutableStateOf(true) }
    var libraryTab  by remember { mutableStateOf(0) }
    var searchText  by remember { mutableStateOf("") }
    var eqPreset    by remember { mutableStateOf(0) }
    var actionsTrack by remember { mutableStateOf<Track?>(null) }

    val currentIdx = state.tracks.indexOfFirst { it.id == state.currentTrack?.id }
    val prevTrack  = if (currentIdx > 0) state.tracks[currentIdx - 1] else null
    val nextTrack  = if (currentIdx in 0 until state.tracks.size - 1) state.tracks[currentIdx + 1] else null
    val currentMs  = state.currentTrack?.let { it.durationMs * state.positionStep / 20L } ?: 0L
    val totalMs    = state.currentTrack?.durationMs ?: 0L

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        SovietHeader(viewModel, Modifier.padding(horizontal = 8.dp, vertical = 6.dp))

        if (isLandscape) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp)) {
                    SovietNowPlayingCard(state, prevTrack, nextTrack, currentMs, totalMs)
                    Spacer(Modifier.height(10.dp))
                    SovietVuSeek(state, currentMs, totalMs, viewModel)
                    Spacer(Modifier.height(8.dp))
                    TransportButtonRow(state.isPlaying, { viewModel.previousTrack() }, { viewModel.playPause() }, { viewModel.nextTrack() }, { viewModel.stop() },
                        labelPrev = "◀◀\nPREV", labelPlay = if (state.isPlaying) "▐▐\nPAUSE" else "▶\nPLAY",
                        labelNext = "▶▶\nNEXT", labelStop = "■\nSTOP", fontSize = 10)
                    Spacer(Modifier.height(8.dp))
                    SovietVolContent(state.volume, viewModel)
                    Spacer(Modifier.height(8.dp))
                    SovietPanelContent(state, eqPreset, viewModel) { eqPreset = it }
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MudBlack))
                val ls = rememberLazyListState()
                Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                    LazyColumn(state = ls, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sovietLibrarySection(
                            state, viewModel, queueOpen, { queueOpen = !queueOpen },
                            libraryOpen, { libraryOpen = !libraryOpen }, libraryTab, { libraryTab = it },
                            searchText, { searchText = it; viewModel.setSearch(it) }, { actionsTrack = it }
                        )
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                    MudScrollbar(ls)
                }
            }
        } else if (state.isExpanded) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp)) {
                SovietNowPlayingCard(state, prevTrack, nextTrack, currentMs, totalMs)
                Spacer(Modifier.height(10.dp))
                SovietVuSeek(state, currentMs, totalMs, viewModel)
                Spacer(Modifier.height(8.dp))
                TransportButtonRow(state.isPlaying, { viewModel.previousTrack() }, { viewModel.playPause() }, { viewModel.nextTrack() }, { viewModel.stop() },
                    labelPrev = "◀◀\nPREV", labelPlay = if (state.isPlaying) "▐▐\nPAUSE" else "▶\nPLAY",
                    labelNext = "▶▶\nNEXT", labelStop = "■\nSTOP", fontSize = 10)
                Spacer(Modifier.height(8.dp))
                SovietVolContent(state.volume, viewModel)
                Spacer(Modifier.height(8.dp))
                SovietPanelContent(state, eqPreset, viewModel) { eqPreset = it }
                Spacer(Modifier.height(8.dp))
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).border(2.dp, MudBlack)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.toggleExpanded() }
                        .padding(vertical = 10.dp)
                ) { TextMMD("TRACK LIST ▼", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = MudBlack) }
            }
        } else {
            val ls = rememberLazyListState()
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(state = ls, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {

                    item { CollapsiblePanel("SIGNAL LEVEL  ·  SEEK", vuOpen, { vuOpen = !vuOpen }) {
                        SovietVuSeek(state, currentMs, totalMs, viewModel)
                    }}

                    item { CollapsiblePanel("NOW PLAYING", nowPlayOpen, { nowPlayOpen = !nowPlayOpen }) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                TextMMD(state.currentTrack?.title?.uppercase() ?: "— NO TRACK —", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MudBlack)
                                Spacer(Modifier.height(3.dp))
                                TextMMD(state.currentTrack?.artist ?: "SELECT A TRACK BELOW", fontSize = 12.sp, color = MudBlack)
                                if (state.currentTrack != null) TextMMD("${formatMs(currentMs)}  /  ${formatMs(totalMs)}", fontSize = 12.sp, color = MudBlack)
                            }
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp).border(1.dp, MudBlack)
                                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.toggleExpanded() }
                            ) { TextMMD("▲", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
                        }
                    }}

                    item { CollapsiblePanel("CONTROLS", ctrlOpen, { ctrlOpen = !ctrlOpen }) {
                        TransportButtonRow(state.isPlaying, { viewModel.previousTrack() }, { viewModel.playPause() }, { viewModel.nextTrack() }, { viewModel.stop() },
                            labelPrev = "◀◀\nPREV", labelPlay = if (state.isPlaying) "▐▐\nPAUSE" else "▶\nPLAY",
                            labelNext = "▶▶\nNEXT", labelStop = "■\nSTOP", fontSize = 10)
                    }}

                    item { CollapsiblePanel("VOLUME", volOpen, { volOpen = !volOpen }) {
                        SovietVolContent(state.volume, viewModel)
                    }}

                    item { CollapsiblePanel("PANEL  ·  BALANCE / EQ / TONE", panelOpen, { panelOpen = !panelOpen }) {
                        SovietPanelContent(state, eqPreset, viewModel) { eqPreset = it }
                    }}

                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextMMD("GOST 12.2.006", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
                            TextMMD("No. 0000001", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
                        }
                        HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
                    }

                    sovietLibrarySection(
                        state, viewModel, queueOpen, { queueOpen = !queueOpen },
                        libraryOpen, { libraryOpen = !libraryOpen }, libraryTab, { libraryTab = it },
                        searchText, { searchText = it; viewModel.setSearch(it) }, { actionsTrack = it }
                    )
                    item { Spacer(Modifier.height(8.dp)) }
                }
                MudScrollbar(ls)
            }
        }

        actionsTrack?.let { t ->
            TrackActionsSheet(
                track = t,
                playlists = state.playlists,
                onPlayNext = { viewModel.playNext(t) },
                onAddToPlaylist = { plId -> viewModel.addToPlaylist(plId, t.id) },
                onCreatePlaylistWithTrack = { name -> viewModel.createPlaylistWithTrack(name, t.id) },
                onDismiss = { actionsTrack = null }
            )
        }
    }
}

@Composable
private fun SovietHeader(viewModel: PlayerViewModel, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().border(4.dp, MudBlack).padding(4.dp).border(1.dp, MudBlack)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val r = 5.dp.toPx(); val p = 9.dp.toPx()
            listOf(Offset(p,p), Offset(size.width-p,p), Offset(p,size.height-p), Offset(size.width-p,size.height-p))
                .forEach { drawCircle(Color.Black, radius = r, center = it, style = Stroke(1.5.dp.toPx())); drawLine(Color.Black, Offset(it.x - r * 0.6f, it.y), Offset(it.x + r * 0.6f, it.y), strokeWidth = 1.5f) }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextMMD("AMPLIFIER", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp, color = MudBlack)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    TextMMD("UM-50", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MudBlack)
                    TextMMD("USSR", fontSize = 10.sp, color = MudBlack)
                }
                ThemeSwitchButton(viewModel)
            }
        }
    }
}

@Composable
private fun SovietNowPlayingCard(state: PlayerUiState, prevTrack: Track?, nextTrack: Track?, currentMs: Long, totalMs: Long) {
    Box(modifier = Modifier.fillMaxWidth().border(4.dp, MudBlack).padding(4.dp).border(1.dp, MudBlack)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val r = 5.dp.toPx(); val p = 9.dp.toPx()
            listOf(Offset(p,p), Offset(size.width-p,p), Offset(p,size.height-p), Offset(size.width-p,size.height-p))
                .forEach { drawCircle(Color.Black, radius = r, center = it, style = Stroke(1.5.dp.toPx())); drawLine(Color.Black, Offset(it.x - r * 0.6f, it.y), Offset(it.x + r * 0.6f, it.y), strokeWidth = 1.5f) }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextMMD("PREVIOUS:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MudBlack)
            TextMMD("◁  ${prevTrack?.title ?: "START OF LIST"}", fontSize = 13.sp, color = MudBlack)
            if (prevTrack != null) TextMMD("   ${prevTrack.artist}  ·  ${formatMs(prevTrack.durationMs)}", fontSize = 12.sp, color = MudBlack)
            HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            Spacer(Modifier.height(10.dp))
            TextMMD(if (state.isPlaying) "● RUN" else "○ STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MudBlack)
            TextMMD(state.currentTrack?.title?.uppercase() ?: "— NO TRACK —", fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = MudBlack)
            Spacer(Modifier.height(3.dp))
            TextMMD(state.currentTrack?.artist?.uppercase() ?: "", fontSize = 13.sp, letterSpacing = 1.sp, color = MudBlack)
            Spacer(Modifier.height(6.dp))
            TextMMD("${formatMs(currentMs)}  /  ${formatMs(totalMs)}  ·  ${"%02d".format(state.positionStep)}/20",
                fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
            Spacer(Modifier.height(10.dp))
            HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            TextMMD("NEXT:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MudBlack)
            TextMMD("▷  ${nextTrack?.title ?: "END OF LIST"}", fontSize = 13.sp, color = MudBlack)
            if (nextTrack != null) TextMMD("   ${nextTrack.artist}  ·  ${formatMs(nextTrack.durationMs)}", fontSize = 12.sp, color = MudBlack)
        }
    }
}

@Composable
private fun SovietVuSeek(state: PlayerUiState, currentMs: Long, totalMs: Long, viewModel: PlayerViewModel) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            TextMMD("SIGNAL LEVEL", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MudBlack)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextMMD(if (state.isPlaying) "● RUN" else "○ STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MudBlack)
                TextMMD("%02d/20".format(state.positionStep), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
            repeat(10) { i ->
                val barStep = (i + 1) * 2
                Box(modifier = Modifier.weight(1f).height(12.dp + (i * 4).dp).border(1.dp, MudBlack)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.seekToStep(barStep) }
                ) { if (state.positionStep >= barStep) Canvas(Modifier.fillMaxSize()) { drawRect(Color.Black) } }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("0", "2", "4", "6", "8", "10").forEach { TextMMD(it, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MudBlack) }
            }
            TextMMD("${formatMs(currentMs)} / ${formatMs(totalMs)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
        }
    }
}

@Composable
private fun SovietVolContent(volume: Int, viewModel: PlayerViewModel) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextMMD("VOL.", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MudBlack)
            Spacer(Modifier.height(2.dp))
            Canvas(modifier = Modifier.size(46.dp)) {
                val cx = size.width / 2; val cy = size.height / 2; val r = (size.minDimension / 2) - 4.dp.toPx()
                drawCircle(Color.Black, radius = r, style = Stroke(2.dp.toPx()))
                repeat(11) { i ->
                    val angle = Math.toRadians(135.0 + 270.0 * i / 10); val inner = r - 4.dp.toPx()
                    drawLine(Color.Black, start = Offset((cx + inner * Math.cos(angle)).toFloat(), (cy + inner * Math.sin(angle)).toFloat()),
                        end = Offset((cx + r * Math.cos(angle)).toFloat(), (cy + r * Math.sin(angle)).toFloat()), strokeWidth = 1.5f)
                }
                val na = Math.toRadians(135.0 + 270.0 * volume / 10); val nl = r - 6.dp.toPx()
                drawLine(Color.Black, start = Offset(cx, cy), end = Offset((cx + nl * Math.cos(na)).toFloat(), (cy + nl * Math.sin(na)).toFloat()), strokeWidth = 2.dp.toPx())
                drawCircle(Color.Black, radius = 3.dp.toPx(), center = Offset(cx, cy))
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp).border(1.dp, MudBlack)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.setVolume(maxOf(0, volume - 1)) }
        ) { TextMMD("−", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(10) { i ->
                    Box(modifier = Modifier.weight(1f).height(28.dp).border(1.dp, MudBlack)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.setVolume(i + 1) }
                    ) { if (i < volume) Canvas(Modifier.fillMaxSize()) { drawRect(Color.Black) } }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextMMD("1", fontSize = 10.sp, color = MudBlack)
                TextMMD("5", fontSize = 10.sp, color = MudBlack)
                TextMMD("10", fontSize = 10.sp, color = MudBlack)
            }
        }
        Spacer(Modifier.width(4.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp).border(1.dp, MudBlack)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.setVolume(minOf(10, volume + 1)) }
        ) { TextMMD("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
    }
}

@Composable
private fun SovietPanelContent(state: PlayerUiState, eqPreset: Int, viewModel: PlayerViewModel, onEqPreset: (Int) -> Unit) {
    val hfOn  = state.eqTreble > 5
    val midOn = state.eqMid    > 5
    val lfOn  = state.eqBass   > 5
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                TextMMD("BALANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MudBlack)
                Spacer(Modifier.height(3.dp))
                MultiSelector(listOf("L", "C", "R"), state.balanceIdx, { viewModel.setBalance(it) }, 11.sp)
            }
            Column {
                TextMMD("EQ PRESET", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MudBlack)
                Spacer(Modifier.height(3.dp))
                MultiSelector(listOf("WARM", "FLAT", "BRIGHT"), eqPreset, { onEqPreset(it); viewModel.applyEqPreset(when (it) { 0 -> 3; 2 -> 4; else -> 0 }) }, 10.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextMMD("TONE:", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MudBlack)
            StyledSwitch(hfOn, "HI", SwitchStyle.LOCK_LEVER)  { viewModel.toggleEqTreble(!hfOn) }
            StyledSwitch(midOn, "MID", SwitchStyle.LOCK_LEVER) { viewModel.toggleEqMid(!midOn) }
            StyledSwitch(lfOn, "LO", SwitchStyle.LOCK_LEVER)  { viewModel.toggleEqBass(!lfOn) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TextMMD("MODE:", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MudBlack)
            StyledSwitch(state.shuffle, "SHUFFLE", SwitchStyle.LOCK_LEVER) { viewModel.toggleShuffle() }
            RepeatButton(state.repeatMode, "REPEAT") { viewModel.cycleRepeat() }
        }
        HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
        SleepTimerControl(state.sleepTimerRemainingSec) { viewModel.setSleepTimer(it) }
    }
}

@Composable
private fun SovietGroupHeader(title: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        TextMMD(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MudBlack)
        TextMMD("$count", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MudBlack)
    }
    HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
}

@Composable
private fun SovietTrackRow(track: Track, isCurrent: Boolean, isFav: Boolean, onClick: () -> Unit, onFav: () -> Unit, onMore: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()
        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextMMD(if (isCurrent) "▶ " else "▷ ", fontSize = 13.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = MudBlack)
        Column(modifier = Modifier.weight(1f)) {
            TextMMD(track.title, fontSize = 14.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = MudBlack)
            TextMMD(track.artist, fontSize = 12.sp, color = MudBlack)
        }
        TextMMD(formatMs(track.durationMs), fontSize = 12.sp, color = MudBlack)
        Box(modifier = Modifier.size(44.dp).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onFav), contentAlignment = Alignment.Center) {
            TextMMD(if (isFav) "★" else "☆", fontSize = 17.sp, color = MudBlack)
        }
        Box(modifier = Modifier.size(44.dp).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onMore), contentAlignment = Alignment.Center) {
            TextMMD("⋯", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MudBlack)
        }
    }
}

private fun LazyListScope.sovietLibrarySection(
    state: PlayerUiState,
    viewModel: PlayerViewModel,
    queueOpen: Boolean, onQueueToggle: () -> Unit,
    libraryOpen: Boolean, onLibraryToggle: () -> Unit,
    libraryTab: Int, onLibraryTab: (Int) -> Unit,
    searchText: String, onSearchChange: (String) -> Unit,
    onMore: (Track) -> Unit
) {
    item { CollapsiblePanel("QUEUE  ·  ${state.queue.size}", queueOpen, onQueueToggle) {
        if (state.queue.isEmpty()) {
            TextMMD("— EMPTY —", fontSize = 12.sp, color = MudBlack)
        } else {
            Column { state.queue.forEach { t ->
                QueueItemRow(t, { viewModel.playTrack(t) }, { viewModel.removeFromQueue(t) })
                HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            } }
        }
    }}

    item { CollapsiblePanel("LIBRARY  ·  ${state.tracks.size} TRACKS", libraryOpen, onLibraryToggle) {
        LibraryControls(searchText, state.sortMode, libraryTab, state.filteredTracks.size,
            onSearchChange, { viewModel.cycleSort() }, onLibraryTab)
    }}

    if (libraryOpen) {
        when (libraryTab) {
            0 -> items(state.filteredTracks, key = { it.id }) { t ->
                SovietTrackRow(t, t.id == state.currentTrack?.id, t.id in state.favorites, { viewModel.playTrack(t) }, { viewModel.toggleFavorite(t.id) }, { onMore(t) })
                HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            }
            1 -> state.albumGroups.forEach { (album, tracks) ->
                item(key = "a_$album") { SovietGroupHeader("◉  ${album.ifBlank { "Unknown Album" }}", tracks.size) }
                items(tracks, key = { "ta_${it.id}" }) { t -> SovietTrackRow(t, t.id == state.currentTrack?.id, t.id in state.favorites, { viewModel.playTrack(t) }, { viewModel.toggleFavorite(t.id) }, { onMore(t) }); HorizontalDividerMMD(thickness = 1.dp, color = MudBlack) }
            }
            2 -> state.artistGroups.forEach { (artist, tracks) ->
                item(key = "ar_$artist") { SovietGroupHeader("♫  ${artist.ifBlank { "Unknown Artist" }}", tracks.size) }
                items(tracks, key = { "tr_${it.id}" }) { t -> SovietTrackRow(t, t.id == state.currentTrack?.id, t.id in state.favorites, { viewModel.playTrack(t) }, { viewModel.toggleFavorite(t.id) }, { onMore(t) }); HorizontalDividerMMD(thickness = 1.dp, color = MudBlack) }
            }
            3 -> {
                item(key = "new_playlist") { NewPlaylistRow { name -> viewModel.createPlaylist(name) } }
                items(state.playlists, key = { "pl_${it.id}" }) { pl ->
                    PlaylistRow(pl, { viewModel.playPlaylist(pl.id) }, { viewModel.deletePlaylist(pl.id) })
                    HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
                }
            }
        }
    }
}
