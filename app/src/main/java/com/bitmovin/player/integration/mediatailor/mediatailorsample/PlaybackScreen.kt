package com.bitmovin.player.integration.mediatailor.mediatailorsample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitmovin.player.integration.mediatailor.mediatailorsample.ui.PlayerView

@Composable
fun PlaybackScreen(
    viewModel: PlaybackViewModel = viewModel<PlaybackViewModel>(),
) {
    val uiState = viewModel.uiState.collectAsState().value

    Column {
        PlayerView(
            Modifier.height(200.dp),
            viewModel.player,
        )

        if (uiState.errorMessage != null) {
            Text(text = "Error: ${uiState.errorMessage}")
        }
        if (uiState.nextAdBreakMessage != null) {
            Text(text = uiState.nextAdBreakMessage)
        }
        if (uiState.currentAdBreakMessage != null) {
            Text(text = uiState.currentAdBreakMessage)
        }
        if (uiState.currentAdMessage != null) {
            Text(text = uiState.currentAdMessage)
        }
    }
}