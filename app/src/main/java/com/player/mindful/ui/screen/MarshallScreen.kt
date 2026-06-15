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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    var queueOpen      by remember { mutableStateOf(true) }
    var libraryTab     by remember { mutableStateOf(0) }
    var searchText     by remember { mutableStateOf("") }
    var actionsTrack   by remember { mutableStateOf<Track?>(null) }

    val currentIdx = state.tracks.indexOfFirst { it.id == state.currentTrack?.id }
    val prevTrack  = if (currentIdx > 0) state.tracks[currentIdx - 1] else null
    val nextTrack  = if (currentIdx in 0 until state.tracks.size - 1) state.tracks[currentIdx + 1] else null
    val currentMs  = state.currentTrack?.let { it.durationMs * state.positionStep / 20L } ?: 0L
    val totalMs    = state.currentTrack?.durationMs ?: 0L

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        MarshallHeader(viewModel, Modifier.padding(horizontal = 8.dp, vertical = 6.dp))

        if (isLandscape) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp)) {
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
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MudBlack))
                val ls = rememberLazyListState()
                Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                    LazyColumn(state = ls, modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        marshallLibrarySection(
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
                ) { TextMMD("▼  TRACK LIST", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = MudBlack) }
            }
        } else {
            val ls = rememberLazyListState()
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(state = ls, modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {

                    item { CollapsiblePanel("NOW PLAYING", nowPlayingOpen, { nowPlayingOpen = !nowPlayingOpen }) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                TextMMD(state.currentTrack?.title?.uppercase() ?: "— NO TRACK SELECTED —", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MudBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(3.dp))
                                TextMMD(state.currentTrack?.artist?.uppercase() ?: "TAP A TRACK BELOW TO PLAY", fontSize = 12.sp, letterSpacing = 1.sp, color = MudBlack)
                                if (state.currentTrack != null) TextMMD("${formatMs(currentMs)}  /  ${formatMs(totalMs)}", fontSize = 12.sp, color = MudBlack)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.heightIn(min = 44.dp).border(1.dp, MudBlack)
                                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.toggleExpanded() }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) { TextMMD("▲ FULL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
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

                    marshallLibrarySection(
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
                    TextMMD("MODEL 1960", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = MudBlack)
                    TextMMD("RATED 100W", fontSize = 10.sp, letterSpacing = 1.sp, color = MudBlack)
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
            TextMMD("◁  ${prevTrack?.title ?: "START OF LIST"}", fontSize = 12.sp, color = MudBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (prevTrack != null) TextMMD("     ${prevTrack.artist}  ·  ${formatMs(prevTrack.durationMs)}", fontSize = 11.sp, color = MudBlack)
            HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            Spacer(Modifier.height(14.dp))
            TextMMD(state.currentTrack?.title?.uppercase() ?: "— NO TRACK —", fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = MudBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            TextMMD(state.currentTrack?.artist?.uppercase() ?: "", fontSize = 13.sp, letterSpacing = 2.sp, color = MudBlack)
            Spacer(Modifier.height(8.dp))
            TextMMD("${formatMs(currentMs)}  ·  ${formatMs(totalMs)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MudBlack)
            Spacer(Modifier.height(14.dp))
            HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            TextMMD("▷  ${nextTrack?.title ?: "END OF LIST"}", fontSize = 12.sp, color = MudBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (nextTrack != null) TextMMD("     ${nextTrack.artist}  ·  ${formatMs(nextTrack.durationMs)}", fontSize = 11.sp, color = MudBlack)
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
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp).border(1.dp, MudBlack)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.setVolume(maxOf(0, volume - 1)) }
        ) { TextMMD("−", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
        Spacer(Modifier.width(4.dp))
        Row(modifier = Modifier.weight(1f)) {
            repeat(10) { i ->
                Box(modifier = Modifier.weight(1f).height(20.dp).padding(1.dp).border(1.dp, MudBlack)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.setVolume(i + 1) }
                ) { if (i < volume) Canvas(Modifier.fillMaxSize()) { drawRect(Color.Black) } }
            }
        }
        Spacer(Modifier.width(4.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp).border(1.dp, MudBlack)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.setVolume(minOf(10, volume + 1)) }
        ) { TextMMD("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
    }
}

@Composable
private fun MarshallAmpContent(state: PlayerUiState, viewModel: PlayerViewModel) {
    var channel by remember { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                TextMMD("EQ PRESET", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = MudBlack)
                Spacer(Modifier.height(4.dp))
                MultiSelector(listOf("FLAT", "ROCK", "LEAD"), channel, { channel = it; viewModel.applyEqPreset(it) }, 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextMMD("BASS BOOST", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MudBlack)
                Spacer(Modifier.height(4.dp))
                StyledSwitch(state.bassBoostOn, "", SwitchStyle.AMP_FLIP) { viewModel.setBassBoost(!state.bassBoostOn) }
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            StyledSwitch(state.shuffle, "SHUFFLE", SwitchStyle.AMP_FLIP) { viewModel.toggleShuffle() }
            RepeatButton(state.repeatMode, "REPEAT") { viewModel.cycleRepeat() }
        }
        HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
        SleepBatteryControl(state.sleepTimerRemainingSec, state.sleepTimerTotalSec) { viewModel.setSleepTimer(it) }
    }
}

@Composable
private fun AmpKnob(label: String, value: Int, onSet: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextMMD(label, fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, color = MudBlack)
        Spacer(Modifier.height(2.dp))
        Box {
            Canvas(modifier = Modifier.size(44.dp)) {
                val ins = Offset(4.dp.toPx(), 4.dp.toPx()); val az = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx())
                drawArc(Color.Black, 135f, 270f, false, style = Stroke(1.5.dp.toPx()), topLeft = ins, size = az)
                drawArc(Color.Black, 135f, 270f * value / 10f, false, style = Stroke(3.dp.toPx()), topLeft = ins, size = az)
            }
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                TextMMD("$value", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MudBlack)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).border(1.dp, MudBlack)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSet(maxOf(0, value - 1)) }
            ) { TextMMD("−", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).border(1.dp, MudBlack)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSet(minOf(10, value + 1)) }
            ) { TextMMD("+", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MudBlack) }
        }
    }
}

@Composable
private fun GroupHeader(title: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TextMMD(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MudBlack)
        TextMMD("$count", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MudBlack)
    }
    DottedDivider()
}

@Composable
private fun MarshallTrackRow(track: Track, isCurrent: Boolean, isFav: Boolean, onClick: () -> Unit, onFav: () -> Unit, onMore: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()
        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
        .padding(horizontal = 10.dp, vertical = ROW_VPAD),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextMMD(if (isCurrent) "▶ " else "▷ ", fontSize = 13.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = MudBlack)
        Column(modifier = Modifier.weight(1f)) {
            TextMMD(track.title, fontSize = 14.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = MudBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextMMD(track.artist, fontSize = 12.sp, color = MudBlack, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(6.dp))
        TextMMD(formatMs(track.durationMs), fontSize = 12.sp, color = MudBlack)
        Box(modifier = Modifier.size(44.dp).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onFav), contentAlignment = Alignment.Center) {
            TextMMD(if (isFav) "★" else "☆", fontSize = 17.sp, color = MudBlack)
        }
        Box(modifier = Modifier.size(44.dp).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onMore), contentAlignment = Alignment.Center) {
            TextMMD("⋯", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MudBlack)
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

private fun LazyListScope.marshallLibrarySection(
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
            TextMMD("— EMPTY —", fontSize = 11.sp, color = MudBlack)
        } else {
            Column { state.queue.forEach { t ->
                QueueItemRow(t, { viewModel.playTrack(t) }, { viewModel.removeFromQueue(t) })
                DottedDivider()
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
                MarshallTrackRow(t, t.id == state.currentTrack?.id, t.id in state.favorites, { viewModel.playTrack(t) }, { viewModel.toggleFavorite(t.id) }, { onMore(t) })
                DottedDivider()
            }
            1 -> state.albumGroups.forEach { (album, tracks) ->
                item(key = "alb_$album") { GroupHeader("◉  ${album.ifBlank { "Unknown Album" }}", tracks.size) }
                items(tracks, key = { "ta_${it.id}" }) { t -> MarshallTrackRow(t, t.id == state.currentTrack?.id, t.id in state.favorites, { viewModel.playTrack(t) }, { viewModel.toggleFavorite(t.id) }, { onMore(t) }); DottedDivider() }
            }
            2 -> state.artistGroups.forEach { (artist, tracks) ->
                item(key = "art_$artist") { GroupHeader("♪  ${artist.ifBlank { "Unknown Artist" }}", tracks.size) }
                items(tracks, key = { "tr_${it.id}" }) { t -> MarshallTrackRow(t, t.id == state.currentTrack?.id, t.id in state.favorites, { viewModel.playTrack(t) }, { viewModel.toggleFavorite(t.id) }, { onMore(t) }); DottedDivider() }
            }
            3 -> {
                item(key = "new_playlist") { NewPlaylistRow { name -> viewModel.createPlaylist(name) } }
                items(state.playlists, key = { "pl_${it.id}" }) { pl ->
                    PlaylistRow(pl, { viewModel.playPlaylist(pl.id) }, { viewModel.deletePlaylist(pl.id) })
                    DottedDivider()
                }
            }
        }
    }
}
