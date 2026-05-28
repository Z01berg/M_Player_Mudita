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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.player.mindful.ui.theme.MudBlack
import com.player.mindful.viewmodel.PlayerViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SynthScreen(viewModel: PlayerViewModel) {
    val tracks by viewModel.tracks.collectAsState()
    val current by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val posStep by viewModel.positionStep.collectAsState()
    val volume by viewModel.volume.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    ) {
        // Header module — dashed border, synth model label
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MudBlack)
                .padding(1.dp)
                .combinedClickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                    onLongClick = { viewModel.cycleTheme() }
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = Color.Black,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextMMD(
                    text = "SYNTH PLAYER",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 3.sp,
                    color = MudBlack
                )
                TextMMD(
                    text = "v1.0",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MudBlack
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // LCD display module — segmented-style track info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MudBlack)
                .padding(8.dp)
        ) {
            Column {
                // Simulated LCD row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextMMD(
                        text = if (isPlaying) "▶ PLAY" else "■ STOP",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MudBlack
                    )
                    TextMMD(
                        text = "POS: %02d/20".format(posStep),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MudBlack
                    )
                    TextMMD(
                        text = "VOL: %02d".format(volume),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MudBlack
                    )
                }
                HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
                Spacer(Modifier.height(4.dp))
                TextMMD(
                    text = current?.title ?: "< NO TRACK LOADED >",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MudBlack
                )
                TextMMD(
                    text = current?.artist ?: "",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MudBlack
                )
                TextMMD(
                    text = current?.album ?: "",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MudBlack
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Step sequencer-style seek — 20 cells in 2 rows of 10
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MudBlack)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            TextMMD(
                "SEQ",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MudBlack
            )
            repeat(2) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(10) { col ->
                        val step = row * 10 + col
                        val filled = step < posStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .border(1.dp, MudBlack)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { viewModel.seekToStep(step + 1) }
                        ) {
                            if (filled) {
                                Canvas(Modifier.fillMaxSize()) { drawRect(Color.Black) }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Controls — monospace labels in bordered cells
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("REW" to {}, "PLAY" to { viewModel.playPause() }, "FWD" to {}, "STOP" to { viewModel.stop() })
                .forEach { (label, action) ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MudBlack)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = action
                            )
                            .padding(vertical = 8.dp)
                    ) {
                        TextMMD(
                            text = label,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MudBlack
                        )
                    }
                }
        }

        Spacer(Modifier.height(6.dp))

        // Volume module — horizontal bar graph
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MudBlack)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextMMD("VOL", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MudBlack)
            repeat(10) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .border(1.dp, MudBlack)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { viewModel.setVolume(i + 1) }
                ) {
                    if (i < volume) {
                        Canvas(Modifier.fillMaxSize()) { drawRect(Color.Black) }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Grid divider
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            val step = 8.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(Color.Black, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
        }

        Spacer(Modifier.height(4.dp))

        // Track list
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(tracks, key = { it.id }) { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { viewModel.playTrack(track) }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val isCurrent = track.id == current?.id
                    TextMMD(
                        text = "${if (isCurrent) ">" else " "} ${track.title}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = MudBlack,
                        modifier = Modifier.weight(1f)
                    )
                    TextMMD(
                        text = formatMs(track.durationMs),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MudBlack
                    )
                }
                HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            }
        }
    }
}
