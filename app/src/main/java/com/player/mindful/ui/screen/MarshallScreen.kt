package com.player.mindful.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.player.mindful.model.Track
import com.player.mindful.ui.theme.MudBlack
import com.player.mindful.ui.theme.MudWhite
import com.player.mindful.viewmodel.PlayerViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarshallScreen(viewModel: PlayerViewModel) {
    val tracks by viewModel.tracks.collectAsState()
    val current by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val posStep by viewModel.positionStep.collectAsState()
    val volume by viewModel.volume.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Faceplate header — double border, stencil title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, MudBlack)
                .padding(3.dp)
                .border(1.dp, MudBlack)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .combinedClickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                    onLongClick = { viewModel.cycleTheme() }
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextMMD(
                    text = "MARSHALL",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    color = MudBlack
                )
                TextMMD(
                    text = "MODEL 1960",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MudBlack
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Now playing panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MudBlack)
                .padding(10.dp)
        ) {
            Column {
                TextMMD(
                    text = current?.title?.uppercase() ?: "— NO TRACK —",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MudBlack
                )
                Spacer(Modifier.height(2.dp))
                TextMMD(
                    text = current?.artist?.uppercase() ?: "",
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = MudBlack
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Seek bar — 20 discrete segments
        SeekStrip(posStep = posStep, onSeek = { viewModel.seekToStep(it) })

        Spacer(Modifier.height(8.dp))

        // Controls row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MudBlack)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton("◀◀") { /* prev */ }
            ControlButton(if (isPlaying) "▐▐" else "▶") { viewModel.playPause() }
            ControlButton("▶▶") { /* next */ }
            ControlButton("■") { viewModel.stop() }
        }

        Spacer(Modifier.height(8.dp))

        // Volume — knob arc drawn as stacked bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MudBlack)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextMMD("VOL", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = MudBlack)
            Spacer(Modifier.width(8.dp))
            KnobArc(value = volume, max = 10, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(8.dp))
            Row {
                repeat(10) { i ->
                    val filled = i < volume
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(14.dp)
                            .padding(1.dp)
                            .border(1.dp, MudBlack)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { viewModel.setVolume(i + 1) }
                    ) {
                        if (filled) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(color = Color.Black)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDividerMMD(thickness = 2.dp, color = MudBlack)
        Spacer(Modifier.height(4.dp))

        // Speaker cloth dot-grid texture strip
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            val dotR = 1.5.dp.toPx()
            val spacingX = 8.dp.toPx()
            val spacingY = 6.dp.toPx()
            var y = spacingY / 2
            while (y < size.height) {
                var x = spacingX / 2
                while (x < size.width) {
                    drawCircle(color = Color.Black, radius = dotR, center = Offset(x, y))
                    x += spacingX
                }
                y += spacingY
            }
        }

        HorizontalDividerMMD(thickness = 2.dp, color = MudBlack)
        Spacer(Modifier.height(4.dp))

        // Track list
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(tracks, key = { it.id }) { track ->
                TrackRow(track = track, isCurrent = track.id == current?.id) {
                    viewModel.playTrack(track)
                }
                HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            }
        }
    }
}

@Composable
private fun SeekStrip(posStep: Int, onSeek: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MudBlack)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(20) { i ->
            val filled = i < posStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
                    .border(1.dp, MudBlack)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSeek(i + 1) }
            ) {
                if (filled) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun KnobArc(value: Int, max: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 3.dp.toPx())
        val sweep = 270f * value / max
        drawArc(
            color = Color.Black,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 2.dp.toPx()),
            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
            size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx())
        )
        drawArc(
            color = Color.Black,
            startAngle = 135f,
            sweepAngle = sweep,
            useCenter = false,
            style = stroke,
            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
            size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx())
        )
    }
}

@Composable
private fun ControlButton(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .border(1.dp, MudBlack)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        TextMMD(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MudBlack)
    }
}

@Composable
private fun TrackRow(track: Track, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCurrent) {
            TextMMD("▶ ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MudBlack)
        }
        Column(modifier = Modifier.weight(1f)) {
            TextMMD(
                text = track.title,
                fontSize = 12.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = MudBlack
            )
            TextMMD(
                text = track.artist,
                fontSize = 10.sp,
                color = MudBlack
            )
        }
        TextMMD(
            text = formatMs(track.durationMs),
            fontSize = 10.sp,
            color = MudBlack
        )
    }
}

