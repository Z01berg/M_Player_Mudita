package com.player.mindful.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.player.mindful.model.PlayerTheme
import com.player.mindful.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val state by viewModel.state.collectAsState()
    when (state.theme) {
        PlayerTheme.MARSHALL -> MarshallScreen(viewModel)
        PlayerTheme.SYNTH    -> SynthScreen(viewModel)
        PlayerTheme.SOVIET   -> SovietScreen(viewModel)
    }
}
