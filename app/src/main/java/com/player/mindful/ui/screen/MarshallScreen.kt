package com.player.mindful.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun MarshallScreen(viewModel: PlayerViewModel) {
    val state by viewModel.state.collectAsState()

    var nowPlayingOpen by remember { mutableStateOf(true) }
    var seekOpen       by remember { mutableStateOf(true) }
    var controlsOpen   by remember { mutableStateOf(true) }
    var volumeOpen     by remember { mutableStateOf(true) }
    var ampOpen        by remember { mutableStateOf(false) }
    var playbackOpen   by remember { mutableStateOf(false) }
    var libraryOpen    by remember { mutableStateOf(true) }
    var libraryTab     by remember { mutableStateOf(0) }
    var searchText     by remember { mutableStateOf("") }

    val currentIdx = state.tracks.indexOfFirst { it.id == state.currentTrack?.id }
    val prevTrack  = if (currentIdx > 0) state.tracks[currentIdx - 1] else null
    val nextTrack  = if (currentIdx in 0 until state.tracks.size - 1) state.tracks[currentIdx + 1] else null
    val currentMs  = state.currentTrack?.let { it.durationMs * state.positionStep / 20L } ?: 0L
    val totalMs    = state.currentTrack?.durationMs ?: 0L

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        MarshallHeader(viewModel, Modifier.padding(horizontal = 8.dp, vertical = 6.dp))

        if (state.isExpanded) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp)) {
                MarshallNowPlayingCard(state, prevTrack, nextTrack, currentMs, totalMs)
                Spacer(Modifier.height(10.dp))
                MarshallSeekContent(state, currentMs, totalMs, viewModel)
                Spacer(Modifier.height(8.dp))
                TransportButtonRow(state.isPlaying, { viewModel.previousTrack() }, { viewModel.playPause() }, { viewModel.nextTrack() }, { viewModel.stop() }, fontSize = 16)
                Spacer(Modifier.height(8.dp))
                MarshallVolumeContent(state.volume, viewModel)
                Spacer(Modifier.height(8.dp))
                MarshallAmpContent(state, viewModel)
                Spacer(Modifier.height(8.dp))
                MarshallPlaybackContent(state, viewModel)
                Spacer(Modifier.height(8.dp))
                HorizontalDividerMMD(thickness = 2.dp, color = MudBlack)
                Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) { drawDots() }
                HorizontalDividerMMD(thickness = 2.dp, color = MudBlack)
                Spacer(Modifier.height(8.dp))
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().border(1.dp, MudBlack)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.toggleExpanded() }
                        .padding(vertical = 10.dp)
                ) { TextMMD("▼  TRACK LIST", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = MudBlack) }
            }
        } else {
            val ls = rememberLazyListState()
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(state = ls, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {

                    item { CollapsiblePanel("NOW PLAYING", nowPlayingOpen, { nowPlayingOpen = !nowPlayingOpen }) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                TextMMD(state.currentTrack?.title?.uppercase() ?: "— NO TRACK SELECTED —", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MudBlack)
                                Spacer(Modifier.height(3.dp))
                                TextMMD(state.currentTrack?.artist?.uppercase() ?: "TAP A TRACK BELOW TO PLAY", fontSize = 11.sp, letterSpacing = 1.sp, color = MudBlack)
                                if (state.currentTrack != null) TextMMD("${formatMs(currentMs)}  /  ${formatMs(totalMs)}", fontSize = 10.sp, color = MudBlack)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.border(1.dp, MudBlack)
                                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.toggleExpanded() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) { TextMMD("▲ FULL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
                        }
                    }}

                    item { CollapsiblePanel("SEEK", seekOpen, { seekOpen = !seekOpen }) {
                        MarshallSeekContent(state, currentMs, totalMs, viewModel)
                    }}

                    item { CollapsiblePanel("CONTROLS", controlsOpen, { controlsOpen = !controlsOpen }) {
                        TransportButtonRow(state.isPlaying, { viewModel.previousTrack() }, { viewModel.playPause() }, { viewModel.nextTrack() }, { viewModel.stop() }, fontSize = 16)
                    }}

                    item { CollapsiblePanel("VOLUME", volumeOpen, { volumeOpen = !volumeOpen }) {
                        MarshallVolumeContent(state.volume, viewModel)
                    }}

                    item { CollapsiblePanel("AMP CONTROLS  ·  EQ", ampOpen, { ampOpen = !ampOpen }) {
                        MarshallAmpContent(state, viewModel)
                    }}

                    item { CollapsiblePanel("PLAYBACK  ·  SHUFFLE / REPEAT", playbackOpen, { playbackOpen = !playbackOpen }) {
                        MarshallPlaybackContent(state, viewModel)
                    }}

                    item {
                        HorizontalDividerMMD(thickness = 2.dp, color = MudBlack)
                        Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) { drawDots() }
                        HorizontalDividerMMD(thickness = 2.dp, color = MudBlack)
                    }

                    item { CollapsiblePanel("LIBRARY  ·  ${state.tracks.size} TRACKS", libraryOpen, { libraryOpen = !libraryOpen }) {
                        LibraryControls(searchText, state.sortMode, libraryTab, state.filteredTracks.size,
                            { searchText = it; viewModel.setSearch(it) }, { viewModel.cycleSort() }, { libraryTab = it })
                    }}

                    if (libraryOpen) {
                        when (libraryTab) {
                            0 -> items(state.filteredTracks, key = { it.id }) { t ->
                                MarshallTrackRow(t, t.id == state.currentTrack?.id, t.id in state.favorites, { viewModel.playTrack(t) }, { viewModel.toggleFavorite(t.id) })
                                HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
                            }
                            1 -> state.albumGroups.forEach { (album, tracks) ->
                                item(key = "alb_$album") { GroupHeader("◉  ${album.ifBlank { "Unknown Album" }}", tracks.size) }
                                items(tracks, key = { "ta_${it.id}" }) { t -> MarshallTrackRow(t, t.id == state.currentTrack?.id, t.id in state.favorites, { viewModel.playTrack(t) }, { viewModel.toggleFavorite(t.id) }); HorizontalDividerMMD(thickness = 1.dp, color = MudBlack) }
                            }
                            2 -> state.artistGroups.forEach { (artist, tracks) ->
                                item(key = "art_$artist") { GroupHeader("♪  ${artist.ifBlank { "Unknown Artist" }}", tracks.size) }
                                items(tracks, key = { "tr_${it.id}" }) { t -> MarshallTrackRow(t, t.id == state.currentTrack?.id, t.id in state.favorites, { viewModel.playTrack(t) }, { viewModel.toggleFavorite(t.id) }); HorizontalDividerMMD(thickness = 1.dp, color = MudBlack) }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                MudScrollbar(ls)
            }
        }
    }
}

@Composable
private fun MarshallHeader(viewModel: PlayerViewModel, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().border(3.dp, MudBlack).padding(3.dp).border(1.dp, MudBlack)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val r = 4.dp.toPx(); val p = 9.dp.toPx()
            listOf(Offset(p,p), Offset(size.width-p,p), Offset(p,size.height-p), Offset(size.width-p,size.height-p))
                .forEach { drawCircle(Color.Black, radius = r, center = it) }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextMMD("MARSHALL", fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp, color = MudBlack)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    TextMMD("MODEL 1960", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = MudBlack)
                    TextMMD("RATED 100W", fontSize = 7.sp, letterSpacing = 1.sp, color = MudBlack)
                }
                ThemeSwitchButton(viewModel)
            }
        }
    }
}

@Composable
private fun MarshallNowPlayingCard(state: PlayerUiState, prevTrack: Track?, nextTrack: Track?, currentMs: Long, totalMs: Long) {
    Box(modifier = Modifier.fillMaxWidth().border(3.dp, MudBlack).padding(3.dp).border(1.dp, MudBlack)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val r = 4.dp.toPx(); val p = 9.dp.toPx()
            listOf(Offset(p,p), Offset(size.width-p,p), Offset(p,size.height-p), Offset(size.width-p,size.height-p))
                .forEach { drawCircle(Color.Black, radius = r, center = it) }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            TextMMD("◁  ${prevTrack?.title ?: "START OF LIST"}", fontSize = 11.sp, color = MudBlack)
            if (prevTrack != null) TextMMD("     ${prevTrack.artist}  ·  ${formatMs(prevTrack.durationMs)}", fontSize = 9.sp, color = MudBlack)
            HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            Spacer(Modifier.height(14.dp))
            TextMMD(state.currentTrack?.title?.uppercase() ?: "— NO TRACK —", fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = MudBlack)
            Spacer(Modifier.height(4.dp))
            TextMMD(state.currentTrack?.artist?.uppercase() ?: "", fontSize = 13.sp, letterSpacing = 2.sp, color = MudBlack)
            Spacer(Modifier.height(8.dp))
            TextMMD("${formatMs(currentMs)}  ·  ${formatMs(totalMs)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MudBlack)
            Spacer(Modifier.height(14.dp))
            HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            TextMMD("▷  ${nextTrack?.title ?: "END OF LIST"}", fontSize = 11.sp, color = MudBlack)
            if (nextTrack != null) TextMMD("     ${nextTrack.artist}  ·  ${formatMs(nextTrack.durationMs)}", fontSize = 9.sp, color = MudBlack)
        }
    }
}

@Composable
private fun MarshallSeekContent(state: PlayerUiState, currentMs: Long, totalMs: Long, viewModel: PlayerViewModel) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(20) { i ->
                Box(modifier = Modifier.weight(1f).height(14.dp).border(1.dp, MudBlack)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.seekToStep(i + 1) }
                ) { if (i < state.positionStep) Canvas(Modifier.fillMaxSize()) { drawRect(Color.Black) } }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextMMD(formatMs(currentMs), fontSize = 11.sp, color = MudBlack)
            TextMMD(formatMs(totalMs), fontSize = 11.sp, color = MudBlack)
        }
    }
}

@Composable
private fun MarshallVolumeContent(volume: Int, viewModel: PlayerViewModel) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextMMD("VOL", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = MudBlack)
        Spacer(Modifier.width(8.dp))
        Canvas(modifier = Modifier.size(42.dp)) {
            val ins = Offset(4.dp.toPx(), 4.dp.toPx()); val az = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx())
            drawArc(Color.Black, 135f, 270f, false, style = Stroke(2.dp.toPx()), topLeft = ins, size = az)
            drawArc(Color.Black, 135f, 270f * volume / 10f, false, style = Stroke(3.dp.toPx()), topLeft = ins, size = az)
        }
        Spacer(Modifier.width(6.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.border(1.dp, MudBlack)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.setVolume(maxOf(0, volume - 1)) }
            .padding(horizontal = 6.dp, vertical = 5.dp)
        ) { TextMMD("−", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
        Spacer(Modifier.width(4.dp))
        Row(modifier = Modifier.weight(1f)) {
            repeat(10) { i ->
                Box(modifier = Modifier.weight(1f).height(16.dp).padding(1.dp).border(1.dp, MudBlack)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.setVolume(i + 1) }
                ) { if (i < volume) Canvas(Modifier.fillMaxSize()) { drawRect(Color.Black) } }
            }
        }
        Spacer(Modifier.width(4.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.border(1.dp, MudBlack)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.setVolume(minOf(10, volume + 1)) }
            .padding(horizontal = 6.dp, vertical = 5.dp)
        ) { TextMMD("+", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
    }
}

@Composable
private fun MarshallAmpContent(state: PlayerUiState, viewModel: PlayerViewModel) {
    var channel by remember { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                TextMMD("EQ PRESET", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = MudBlack)
                Spacer(Modifier.height(4.dp))
                MultiSelector(listOf("FLAT", "ROCK", "LEAD"), channel, { channel = it; viewModel.applyEqPreset(it) }, 9.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextMMD("BASS BOOST", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MudBlack)
                Spacer(Modifier.height(4.dp))
                PhysicalSwitch(state.bassBoostOn, "") { viewModel.setBassBoost(!state.bassBoostOn) }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            AmpKnob("TREBLE", state.eqTreble) { viewModel.setEqTreble(it) }
            AmpKnob("MID",    state.eqMid)    { viewModel.setEqMid(it) }
            AmpKnob("BASS",   state.eqBass)   { viewModel.setEqBass(it) }
            AmpKnob("PRES.",  state.eqPresence) { viewModel.setEqPresence(it) }
        }
    }
}

@Composable
private fun MarshallPlaybackContent(state: PlayerUiState, viewModel: PlayerViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        PhysicalSwitch(state.shuffle, "SHUFFLE") { viewModel.toggleShuffle() }
        RepeatButton(state.repeatMode, "REPEAT") { viewModel.cycleRepeat() }
    }
}

@Composable
private fun AmpKnob(label: String, value: Int, onSet: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextMMD(label, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, color = MudBlack)
        Spacer(Modifier.height(2.dp))
        Box {
            Canvas(modifier = Modifier.size(40.dp)) {
                val ins = Offset(4.dp.toPx(), 4.dp.toPx()); val az = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx())
                drawArc(Color.Black, 135f, 270f, false, style = Stroke(1.5.dp.toPx()), topLeft = ins, size = az)
                drawArc(Color.Black, 135f, 270f * value / 10f, false, style = Stroke(3.dp.toPx()), topLeft = ins, size = az)
            }
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                TextMMD("$value", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MudBlack)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.border(1.dp, MudBlack)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSet(maxOf(0, value - 1)) }
                .padding(horizontal = 6.dp, vertical = 4.dp)
            ) { TextMMD("−", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.border(1.dp, MudBlack)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSet(minOf(10, value + 1)) }
                .padding(horizontal = 6.dp, vertical = 4.dp)
            ) { TextMMD("+", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
        }
    }
}

@Composable
private fun GroupHeader(title: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TextMMD(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MudBlack)
        TextMMD("$count", fontSize = 10.sp, color = MudBlack)
    }
    HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
}

@Composable
private fun MarshallTrackRow(track: Track, isCurrent: Boolean, isFav: Boolean, onClick: () -> Unit, onFav: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()
        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextMMD(if (isCurrent) "▶ " else "▷ ", fontSize = 12.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = MudBlack)
        Column(modifier = Modifier.weight(1f)) {
            TextMMD(track.title, fontSize = 13.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = MudBlack)
            TextMMD(track.artist, fontSize = 11.sp, color = MudBlack)
        }
        TextMMD(formatMs(track.durationMs), fontSize = 11.sp, color = MudBlack)
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onFav)) {
            TextMMD(if (isFav) "★" else "☆", fontSize = 14.sp, color = MudBlack)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDots() {
    val dotR = 1.5.dp.toPx(); val sx = 8.dp.toPx(); val sy = 6.dp.toPx()
    var y = sy / 2
    while (y < size.height) {
        var x = sx / 2
        while (x < size.width) { drawCircle(Color.Black, radius = dotR, center = Offset(x, y)); x += sx }
        y += sy
    }
}
