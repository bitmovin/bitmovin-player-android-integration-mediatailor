package com.bitmovin.player.integration.mediatailor.mediatailorsample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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

    Column(Modifier.fillMaxSize()) {
        PlayerView(
            Modifier.height(200.dp),
            viewModel.player,
        )
        Column(
            Modifier.fillMaxSize()
        ) {
            Card(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(8.dp)
                ) {
                    if (uiState.errorMessage != null) {
                        Text(text = "Error: ${uiState.errorMessage}")
                    }
                    if (uiState.playerCurrentTime != null) {
                        Text(text = "Current Time: ${uiState.playerCurrentTime}")
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
                    if (uiState.clickThroughUrl != null) {
                        Button(onClick = {
                            viewModel.triggerClickThrough(uiState.clickThroughUrl)
                        }) {
                            Text(text = "Click Through")
                        }
                    }
                }
            }
            if (uiState.adBreakState != null) {
                val adBreaks = uiState.adBreakState.adBreaks
                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    item {
                        Row {
                            Text(text = uiState.adBreakState.message)
                        }
                    }
                    items(adBreaks) { adBreak ->
                        Button(
                            onClick = { viewModel.seekTo(adBreak) },
                        ) {
                            Text(adBreak.message)
                        }
                    }
                }
            }
        }
    }
}
