package com.bitmovin.player.integration.mediatailor.mediatailorsample

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAssetType
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionManager
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult.Failure
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult.Success
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val nextAdBreakMessage: String? = null,
    val currentAdBreakMessage: String? = null,
    val currentAdMessage: String? = null,
    val errorMessage: String? = null,
)

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    val player = Player(application)
    private val mediaTailorSessionManager = MediaTailorSessionManager(player)
    private val _uiState = MutableStateFlow<UiState>(UiState())
    val uiState: StateFlow<UiState>
        get() = _uiState

    private val nextAdBreak = mediaTailorSessionManager
        .events
        .filterIsInstance<MediaTailorEvent.UpcomingAdBreakUpdate>()
        .map { it.adBreak }
        .stateIn(
            viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
    private val currentAdBreak = mediaTailorSessionManager
        .events
        .filter { it is MediaTailorEvent.AdBreakStarted || it is MediaTailorEvent.AdBreakFinished }
        .map { if (it is MediaTailorEvent.AdBreakStarted) it.adBreak else null }
        .stateIn(
            viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
    private val currentAd = mediaTailorSessionManager
        .events
        .filter { it is MediaTailorEvent.AdStarted || it is MediaTailorEvent.AdFinished }
        .map { it as? MediaTailorEvent.AdStarted }
        .stateIn(
            viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    init {
        viewModelScope.launch {
            val sessionResult = mediaTailorSessionManager.initializeSession(
                MediaTailorSessionConfig(
                    sessionInitUrl = "https://awslive.streamco.video/v1/session/86dfd1144b3bf786fc967f2c3876972e5548ca5d/awslive/out/v1/live/jdub-live-bitmovin02/cmaf-cbcs/hls.m3u8",
                    assetType = MediaTailorAssetType.Linear(),
                )
            )
            when (val result = sessionResult) {
                is Success -> {
                    player.load(SourceConfig.fromUrl(result.manifestUrl))
                    startTrackingSession()
                }

                is Failure -> _uiState.update {
                    UiState(errorMessage = "Error: ${result.message}")
                }
            }
        }
        viewModelScope.launch {
            mediaTailorSessionManager.events.filterIsInstance<MediaTailorEvent.Info>().collect {
                Log.d("PlaybackViewModel", it.message)
            }
        }
        viewModelScope.launch {
            mediaTailorSessionManager.events.filterIsInstance<MediaTailorEvent.Error>().collect {
                Log.e("PlaybackViewModel", it.message)
            }
        }
    }

    private fun startTrackingSession() {
        viewModelScope.launch {
            player.on<PlayerEvent.TimeChanged> {
                updateUiState()
            }
            merge(
                nextAdBreak,
                currentAdBreak,
                currentAd,
            ).collect {
                updateUiState()
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateUiState() {
        val upcomingAdBreakMessage = nextAdBreak.value?.let {
            val upcomingTimeSeconds = (it.scheduleTime - player.currentTime).toInt()

            String.format(nextAdBreakMessageFormat, upcomingTimeSeconds)
        }
        val currentAdBreakMessage = currentAdBreak.value?.let {
            val currentAdBreakTimeLeftSeconds =
                (it.scheduleTime + it.duration - player.currentTime).toInt()

            String.format(currentAdBreakMessageFormat, currentAdBreakTimeLeftSeconds)
        }
        val currentAdMessage = currentAd.value?.let {
            val currentAdTimeLeftSeconds =
                (it.ad.scheduleTime + it.ad.duration - player.currentTime).toInt()
            val adNumber = it.indexInQueue + 1
            val adCount = currentAdBreak.value!!.ads.size

            String.format(
                currentAdMessageFormat,
                adNumber,
                adCount,
                currentAdTimeLeftSeconds,
            )
        }

        _uiState.update {
            UiState(
                nextAdBreakMessage = upcomingAdBreakMessage,
                currentAdBreakMessage = currentAdBreakMessage,
                currentAdMessage = currentAdMessage,
            )
        }
    }

    override fun onCleared() {
        mediaTailorSessionManager.stopSession()
        player.destroy()
    }
}

private const val nextAdBreakMessageFormat = "Next ad break in %d s"
private const val currentAdBreakMessageFormat = "Current ad break ends in %d s"
private const val currentAdMessageFormat = "Playing ad %d / %d.\nEnds in %d s"
