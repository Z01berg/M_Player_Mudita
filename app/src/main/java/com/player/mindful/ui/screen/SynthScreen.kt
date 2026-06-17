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
import androidx.compose.ui.graphics.PathEffect
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
fun SynthScreen(viewModel: PlayerViewModel) {
    val state by viewModel.state.collectAsState()

    var nowPlayingOpen by remember { mutableStateOf(true) }
    var seekOpen       by remember { mutableStateOf(true) }
    var controlsOpen   by remember { mutableStateOf(true) }
    var volumeOpen     by remember { mutableStateOf(true) }
    var moduleOpen     by remember { mutableStateOf(false) }
    var libraryOpen    by remember { mutableStateOf(true) }
    var queueOpen      by remember { mutableStateOf(true) }
    var libraryTab     by remember { mutableStateOf(0) }
    var searchText     by remember { mutableStateOf("") }
    var eqPreset       by remember { mutableStateOf(0) }
    var actionsTrack   by remember { mutableStateOf<Track?>(null) }

    val currentIdx = state.tracks.indexOfFirst { it.id == state.currentTrack?.id }
    val prevTrack  = if (currentIdx > 0) state.tracks[currentIdx - 1] else null
    val nextTrack  = if (currentIdx in 0 until state.tracks.size - 1) state.tracks[currentIdx + 1] else null
    val currentMs  = state.currentTrack?.let { it.durationMs * state.positionStep / 20L } ?: 0L
    val totalMs    = state.currentTrack?.durationMs ?: 0L

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        SynthHeader(viewModel, state, Modifier.padding(horizontal = 8.dp, vertical = 6.dp))

        if (isLandscape) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp)) {
                    SynthNowPlayingCard(state, prevTrack, nextTrack, currentMs, totalMs)
                    Spacer(Modifier.height(10.dp))
                    SynthSeqGrid(state, currentMs, totalMs, viewModel)
                    Spacer(Modifier.height(8.dp))
                    TransportButtonRow(state.isPlaying, { viewModel.previousTrack() }, { viewModel.playPause() }, { viewModel.nextTrack() }, { viewModel.stop() },
                        labelPrev = "REW", labelPlay = if (state.isPlaying) "PAUSE" else "PLAY", labelNext = "FWD", labelStop = "STOP", fontSize = 11)
                    Spacer(Modifier.height(8.dp))
                    SynthVolumeRow(state.volume, viewModel)
                    Spacer(Modifier.height(8.dp))
                    SynthModuleContent(state, eqPreset, viewModel) { eqPreset = it }
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MudBlack))
                val ls = rememberLazyListState()
                Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                    LazyColumn(state = ls, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        synthLibrarySection(
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
                SynthNowPlayingCard(state, prevTrack, nextTrack, currentMs, totalMs)
                Spacer(Modifier.height(10.dp))
                SynthSeqGrid(state, currentMs, totalMs, viewModel)
                Spacer(Modifier.height(8.dp))
                TransportButtonRow(state.isPlaying, { viewModel.previousTrack() }, { viewModel.playPause() }, { viewModel.nextTrack() }, { viewModel.stop() },
                    labelPrev = "REW", labelPlay = if (state.isPlaying) "PAUSE" else "PLAY", labelNext = "FWD", labelStop = "STOP", fontSize = 11)
                Spacer(Modifier.height(8.dp))
                SynthVolumeRow(state.volume, viewModel)
                Spacer(Modifier.height(8.dp))
                SynthModuleContent(state, eqPreset, viewModel) { eqPreset = it }
                Spacer(Modifier.height(8.dp))
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().border(1.dp, MudBlack)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.toggleExpanded() }
                        .padding(vertical = 10.dp)
                ) { TextMMD("[ESC: TRACK LIST]", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack) }
            }
        } else {
            val ls = rememberLazyListState()
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(state = ls, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {

                    item { CollapsiblePanel("NOW PLAYING", nowPlayingOpen, { nowPlayingOpen = !nowPlayingOpen }) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                TextMMD("${if (state.isPlaying) "▶ PLAY" else "■ STOP"}  POS:${"%02d".format(state.positionStep)}/20  VOL:${"%02d".format(state.volume)}",
                                    fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
                                Box(modifier = Modifier.heightIn(min = 44.dp).border(1.dp, MudBlack)
                                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.toggleExpanded() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) { TextMMD("[▲]", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack) }
                            }
                            HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
                            Spacer(Modifier.height(6.dp))
                            TextMMD(state.currentTrack?.title ?: "< NO TRACK LOADED >", fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
                            TextMMD(state.currentTrack?.artist ?: "", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
                            if (state.currentTrack != null) TextMMD("${formatMs(currentMs)} / ${formatMs(totalMs)}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
                        }
                    }}

                    item { CollapsiblePanel("SEQ  ·  SEEK", seekOpen, { seekOpen = !seekOpen }) {
                        SynthSeqGrid(state, currentMs, totalMs, viewModel)
                    }}

                    item { CollapsiblePanel("CONTROLS", controlsOpen, { controlsOpen = !controlsOpen }) {
                        TransportButtonRow(state.isPlaying, { viewModel.previousTrack() }, { viewModel.playPause() }, { viewModel.nextTrack() }, { viewModel.stop() },
                            labelPrev = "REW", labelPlay = if (state.isPlaying) "PAUSE" else "PLAY", labelNext = "FWD", labelStop = "STOP", fontSize = 11)
                    }}

                    item { CollapsiblePanel("VOLUME", volumeOpen, { volumeOpen = !volumeOpen }) {
                        SynthVolumeRow(state.volume, viewModel)
                    }}

                    item { CollapsiblePanel("SYNTH MODULE  ·  SPEED / EQ / PITCH", moduleOpen, { moduleOpen = !moduleOpen }) {
                        SynthModuleContent(state, eqPreset, viewModel) { eqPreset = it }
                    }}

                    item { Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                        val step = 8.dp.toPx(); var x = 0f
                        while (x < size.width) { drawLine(Color.Black, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f); x += step }
                    }}

                    synthLibrarySection(
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
private fun SynthHeader(viewModel: PlayerViewModel, state: PlayerUiState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().border(1.dp, MudBlack).padding(1.dp)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(Color.Black, style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                TextMMD("SYNTH PLAYER", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 3.sp, color = MudBlack)
                TextMMD("STA:${if (state.isPlaying) "PLAY" else "STOP"}  POS:${"%02d".format(state.positionStep)}  VOL:${"%02d".format(state.volume)}",
                    fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextMMD("v1.0", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
                ThemeSwitchButton(viewModel)
            }
        }
    }
}

@Composable
private fun SynthNowPlayingCard(state: PlayerUiState, prevTrack: Track?, nextTrack: Track?, currentMs: Long, totalMs: Long) {
    Box(modifier = Modifier.fillMaxWidth().border(2.dp, MudBlack).padding(2.dp).border(1.dp, MudBlack)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val step = 8.dp.toPx(); var x = 0f
            while (x < size.width) { drawLine(Color(0x18000000), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f); x += step }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextMMD("PREV: ${prevTrack?.title ?: "< BEGIN >"}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
            if (prevTrack != null) TextMMD("      ${prevTrack.artist}  ·  ${formatMs(prevTrack.durationMs)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
            HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            Spacer(Modifier.height(8.dp))
            TextMMD(if (state.isPlaying) "▶ NOW PLAYING" else "■ PAUSED", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
            TextMMD(state.currentTrack?.title ?: "< NO TRACK >", fontSize = 19.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, color = MudBlack)
            TextMMD(state.currentTrack?.artist ?: "", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
            TextMMD("${formatMs(currentMs)} / ${formatMs(totalMs)}  |  POS:${"%02d".format(state.positionStep)}/20",
                fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
            Spacer(Modifier.height(8.dp))
            HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            TextMMD("NEXT: ${nextTrack?.title ?: "< END >"}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
            if (nextTrack != null) TextMMD("      ${nextTrack.artist}  ·  ${formatMs(nextTrack.durationMs)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
        }
    }
}

@Composable
private fun SynthSeqGrid(state: PlayerUiState, currentMs: Long, totalMs: Long, viewModel: PlayerViewModel) {
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, MudBlack).padding(4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextMMD("SEQ", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
            TextMMD("${formatMs(currentMs)} / ${formatMs(totalMs)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
        }
        repeat(2) { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(10) { col ->
                    val step = row * 10 + col
                    Box(modifier = Modifier.weight(1f).height(20.dp).border(1.dp, MudBlack)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.seekToStep(step + 1) }
                    ) { if (step < state.positionStep) Canvas(Modifier.fillMaxSize()) { drawRect(Color.Black) } }
                }
            }
        }
    }
}

@Composable
private fun SynthVolumeRow(volume: Int, viewModel: PlayerViewModel) {
    Row(modifier = Modifier.fillMaxWidth().border(1.dp, MudBlack).padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextMMD("VOL", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).border(1.dp, MudBlack)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.setVolume(maxOf(0, volume - 1)) }
        ) { TextMMD("−", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack) }
        repeat(10) { i ->
            Box(modifier = Modifier.weight(1f).height(24.dp).border(1.dp, MudBlack)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.setVolume(i + 1) }
            ) { if (i < volume) Canvas(Modifier.fillMaxSize()) { drawRect(Color.Black) } }
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).border(1.dp, MudBlack)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.setVolume(minOf(10, volume + 1)) }
        ) { TextMMD("+", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack) }
    }
}

@Composable
private fun SynthModuleContent(state: PlayerUiState, eqPreset: Int, viewModel: PlayerViewModel, onEqPreset: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                TextMMD("SPEED", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
                Spacer(Modifier.height(3.dp))
                MultiSelector(listOf("0.8×", "1.0×", "1.2×"), state.speedIdx, { viewModel.setSpeedIdx(it) }, 11.sp, FontFamily.Monospace)
            }
            Column {
                TextMMD("EQ", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
                Spacer(Modifier.height(3.dp))
                MultiSelector(listOf("WARM", "FLAT", "BRIT"), eqPreset, { onEqPreset(it); viewModel.applyEqPreset(when (it) { 0 -> 3; 2 -> 4; else -> 0 }) }, 10.sp, FontFamily.Monospace)
            }
            Column {
                TextMMD("PITCH", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
                Spacer(Modifier.height(3.dp))
                MultiSelector(listOf("-1", "+0", "+1"), state.pitchIdx, { viewModel.setPitchIdx(it) }, 11.sp, FontFamily.Monospace)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TextMMD("MODE:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
            StyledSwitch(state.shuffle, "SHUFFLE", SwitchStyle.LEVER) { viewModel.toggleShuffle() }
            RepeatButton(state.repeatMode, "REPEAT") { viewModel.cycleRepeat() }
        }
        HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
        SleepTimerControl(state.sleepTimerRemainingSec) { viewModel.setSleepTimer(it) }
    }
}

@Composable
private fun SynthGroupHeader(title: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp)) {
        TextMMD(title, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
    }
    HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
}

@Composable
private fun SynthTrackRow(track: Track, isCurrent: Boolean, isFav: Boolean, onClick: () -> Unit, onFav: () -> Unit, onMore: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()
        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextMMD("${if (isCurrent) "▶" else "▷"} ", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = MudBlack)
        Column(modifier = Modifier.weight(1f)) {
            TextMMD(track.title, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = MudBlack)
            TextMMD(track.artist, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
        }
        TextMMD(formatMs(track.durationMs), fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
        Box(modifier = Modifier.size(44.dp).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onFav), contentAlignment = Alignment.Center) {
            TextMMD(if (isFav) "★" else "☆", fontSize = 17.sp, color = MudBlack)
        }
        Box(modifier = Modifier.size(44.dp).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onMore), contentAlignment = Alignment.Center) {
            TextMMD("⋯", fontSize = 17.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
        }
    }
}

private fun LazyListScope.synthLibrarySection(
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
            TextMMD("< EMPTY >", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
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
                SynthTrackRow(t, t.id == state.currentTrack?.id, t.id in state.favorites, { viewModel.playTrack(t) }, { viewModel.toggleFavorite(t.id) }, { onMore(t) })
                HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            }
            1 -> state.albumGroups.forEach { (album, tracks) ->
                item(key = "a_$album") { SynthGroupHeader("// ${album.ifBlank { "Unknown Album" }} [${tracks.size}]") }
                items(tracks, key = { "ta_${it.id}" }) { t -> SynthTrackRow(t, t.id == state.currentTrack?.id, t.id in state.favorites, { viewModel.playTrack(t) }, { viewModel.toggleFavorite(t.id) }, { onMore(t) }); HorizontalDividerMMD(thickness = 1.dp, color = MudBlack) }
            }
            2 -> state.artistGroups.forEach { (artist, tracks) ->
                item(key = "ar_$artist") { SynthGroupHeader(">> ${artist.ifBlank { "Unknown Artist" }} [${tracks.size}]") }
                items(tracks, key = { "tr_${it.id}" }) { t -> SynthTrackRow(t, t.id == state.currentTrack?.id, t.id in state.favorites, { viewModel.playTrack(t) }, { viewModel.toggleFavorite(t.id) }, { onMore(t) }); HorizontalDividerMMD(thickness = 1.dp, color = MudBlack) }
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
