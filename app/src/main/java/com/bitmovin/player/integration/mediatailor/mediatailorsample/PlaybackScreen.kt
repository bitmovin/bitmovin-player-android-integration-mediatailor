package com.bitmovin.player.integration.mediatailor.mediatailorsample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

        Column(Modifier.padding(16.dp)) {
            if (uiState.errorMessage != null) {
                Text(text = "Error: ${uiState.errorMessage}")
            }
            if (uiState.adBreaksMessage != null) {
                Text(text = uiState.adBreaksMessage)
            }
            if (uiState.nextAdBreakMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(text = uiState.nextAdBreakMessage)
            }
            if (uiState.currentAdBreakMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(text = uiState.currentAdBreakMessage)
            }
            if (uiState.currentAdMessage != null) {
                Text(text = uiState.currentAdMessage)
            }
        }
    }
}
