package com.player.mindful.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.player.mindful.model.PlayerTheme
import com.player.mindful.ui.theme.MudWhite
import com.player.mindful.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val theme by viewModel.theme.collectAsState()

    val modifier = Modifier.fillMaxSize().background(MudWhite)

    when (theme) {
        PlayerTheme.MARSHALL -> MarshallScreen(viewModel)
        PlayerTheme.SYNTH -> SynthScreen(viewModel)
        PlayerTheme.SOVIET -> SovietScreen(viewModel)
    }
}
