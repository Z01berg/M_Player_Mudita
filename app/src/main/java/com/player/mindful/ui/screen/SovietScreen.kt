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
fun SovietScreen(viewModel: PlayerViewModel) {
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
        // Header — triple border, Soviet-style stencil label
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(4.dp, MudBlack)
                .padding(4.dp)
                .border(1.dp, MudBlack)
                .padding(horizontal = 10.dp, vertical = 8.dp)
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
                    text = "ПІДСИЛЮВАЧ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = MudBlack
                )
                Column(horizontalAlignment = Alignment.End) {
                    TextMMD("УМ-50", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MudBlack)
                    TextMMD("СРСР", fontSize = 9.sp, color = MudBlack)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // VU meter — 10 vertical bars as progress
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MudBlack)
                .padding(8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    TextMMD(
                        "РІВЕНЬ СИГНАЛУ",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MudBlack
                    )
                    TextMMD(
                        if (isPlaying) "● РОБ." else "○ СТОП",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MudBlack
                    )
                }
                Spacer(Modifier.height(6.dp))
                // VU bar array
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    repeat(10) { i ->
                        val barStep = (i + 1) * 2
                        val filled = posStep >= barStep
                        val heightDp = 12.dp + (i * 4).dp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(heightDp)
                                .border(1.dp, MudBlack)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { viewModel.seekToStep(barStep) }
                        ) {
                            if (filled) {
                                Canvas(Modifier.fillMaxSize()) {
                                    drawRect(Color.Black)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                // Scale markings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("0", "2", "4", "6", "8", "10").forEach {
                        TextMMD(it, fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = MudBlack)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Track info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MudBlack)
                .padding(8.dp)
        ) {
            Column {
                TextMMD(
                    text = current?.title?.uppercase() ?: "— НЕМАЄ ТРЕКУ —",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MudBlack
                )
                TextMMD(
                    text = current?.artist ?: "",
                    fontSize = 10.sp,
                    color = MudBlack
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Toggle switches row — Soviet panel buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MudBlack)
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SovietButton("◀◀\nПОП.") {}
            SovietButton(if (isPlaying) "▐▐\nПАУЗА" else "▶\nПЛЕЙ") { viewModel.playPause() }
            SovietButton("▶▶\nНАСТ.") {}
            SovietButton("■\nСТОП") { viewModel.stop() }
        }

        Spacer(Modifier.height(8.dp))

        // Volume — dial-style with level markings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MudBlack)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextMMD("ГУЧН.", fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MudBlack)
                Spacer(Modifier.height(2.dp))
                SovietDial(value = volume, max = 10, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(10) { i ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextMMD("1", fontSize = 7.sp, color = MudBlack)
                    TextMMD("5", fontSize = 7.sp, color = MudBlack)
                    TextMMD("10", fontSize = 7.sp, color = MudBlack)
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        HorizontalDividerMMD(thickness = 2.dp, color = MudBlack)
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
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isCurrent = track.id == current?.id
                    if (isCurrent) {
                        TextMMD("▶ ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MudBlack)
                    } else {
                        TextMMD("— ", fontSize = 10.sp, color = MudBlack)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        TextMMD(
                            text = track.title,
                            fontSize = 12.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = MudBlack
                        )
                        TextMMD(text = track.artist, fontSize = 10.sp, color = MudBlack)
                    }
                    TextMMD(formatMs(track.durationMs), fontSize = 10.sp, color = MudBlack)
                }
                HorizontalDividerMMD(thickness = 1.dp, color = MudBlack)
            }
        }
    }
}

@Composable
private fun SovietButton(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .border(2.dp, MudBlack)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        TextMMD(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MudBlack
        )
    }
}

@Composable
private fun SovietDial(value: Int, max: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = (size.minDimension / 2) - 4.dp.toPx()
        // Outer ring
        drawCircle(color = Color.Black, radius = r, style = Stroke(width = 2.dp.toPx()))
        // Tick marks
        repeat(11) { i ->
            val angle = Math.toRadians(135.0 + 270.0 * i / 10)
            val inner = r - 4.dp.toPx()
            val outer = r
            drawLine(
                color = Color.Black,
                start = Offset((cx + inner * Math.cos(angle)).toFloat(), (cy + inner * Math.sin(angle)).toFloat()),
                end = Offset((cx + outer * Math.cos(angle)).toFloat(), (cy + outer * Math.sin(angle)).toFloat()),
                strokeWidth = 1.5f
            )
        }
        // Needle
        val needleAngle = Math.toRadians(135.0 + 270.0 * value / max)
        val needleLen = r - 6.dp.toPx()
        drawLine(
            color = Color.Black,
            start = Offset(cx, cy),
            end = Offset(
                (cx + needleLen * Math.cos(needleAngle)).toFloat(),
                (cy + needleLen * Math.sin(needleAngle)).toFloat()
            ),
            strokeWidth = 2.dp.toPx()
        )
        drawCircle(color = Color.Black, radius = 3.dp.toPx(), center = Offset(cx, cy))
    }
}
