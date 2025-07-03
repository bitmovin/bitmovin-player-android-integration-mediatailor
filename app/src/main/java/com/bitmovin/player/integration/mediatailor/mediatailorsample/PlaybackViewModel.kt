package com.bitmovin.player.integration.mediatailor.mediatailorsample

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAssetType
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionManager
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult.Failure
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult.Success
import com.bitmovin.player.integration.mediatailor.api.TrackingEvent
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

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    val player = Player(application, PlayerConfig(key = TODO("PLAYER_LICENSE_KEY")))
    private val mediaTailorSessionManager = MediaTailorSessionManager(player)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState>
        get() = _uiState

    private val nextAdBreak = mediaTailorSessionManager.events
        .filterIsInstance<MediaTailorEvent.UpcomingAdBreakUpdated>()
        .map { it.adBreak }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val currentAdBreak = mediaTailorSessionManager.events
        .filter { it is MediaTailorEvent.AdBreakStarted || it is MediaTailorEvent.AdBreakFinished }
        .map { if (it is MediaTailorEvent.AdBreakStarted) it.adBreak else null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val currentAd = mediaTailorSessionManager.events
        .filter { it is MediaTailorEvent.AdStarted || it is MediaTailorEvent.AdFinished }
        .map { it as? MediaTailorEvent.AdStarted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val allAdBreaks = mediaTailorSessionManager.events
        .filterIsInstance<MediaTailorEvent.AdBreakScheduleUpdated>()
        .map { it.adBreaks }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            val sessionResult = mediaTailorSessionManager.initializeSession(
                MediaTailorSessionConfig(
                    sessionInitUrl = TODO("SESSION_INIT_URL"),
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
                allAdBreaks,
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
        val clickThroughUrl = currentAd.value?.ad?.clickThroughUrl
        val adBreaksMessage = if (allAdBreaks.value.isNotEmpty()) {
            "Ad Breaks:"
        } else {
            "No ad breaks scheduled."
        }
        val adBreaks = allAdBreaks.value.mapIndexed { index, adBreak ->
            AdBreakState.AdBreak(
                message = adBreaksMessageFormat.format(
                    index + 1,
                    adBreak.scheduleTime.toInt(),
                    (adBreak.scheduleTime + adBreak.duration).toInt(),
                    adBreak.ads.size,
                ),
                startTime = adBreak.scheduleTime,
                endTime = adBreak.scheduleTime + adBreak.duration,
            )
        }

        _uiState.update {
            UiState(
                playerCurrentTime = player.currentTime.toInt().toString(),
                nextAdBreakMessage = upcomingAdBreakMessage,
                currentAdBreakMessage = currentAdBreakMessage,
                currentAdMessage = currentAdMessage,
                clickThroughUrl = clickThroughUrl,
                adBreakState = AdBreakState(
                    message = adBreaksMessage,
                    adBreaks = adBreaks
                ),
            )
        }
    }

    fun triggerClickThrough(url: String) {
        mediaTailorSessionManager.sendTrackingEvent(TrackingEvent.ClickTracking)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = url.toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        getApplication<Application>().startActivity(intent)
    }

    fun seekTo(
        adBreak: AdBreakState.AdBreak,
        offset: Double = 3.0
    ) {
        if (player.isLive) {
            player.timeShift(player.playbackTimeOffsetToAbsoluteTime + adBreak.startTime - offset)
        } else {
            player.seek(adBreak.startTime - offset)
        }
    }

    override fun onCleared() {
        mediaTailorSessionManager.destroy()
        player.destroy()
    }
}

data class AdBreakState(
    val message: String,
    val adBreaks: List<AdBreak>,
) {
    data class AdBreak(
        val message: String,
        val startTime: Double,
        val endTime: Double,
    )
}

data class UiState(
    val playerCurrentTime: String? = null,
    val nextAdBreakMessage: String? = null,
    val currentAdBreakMessage: String? = null,
    val currentAdMessage: String? = null,
    val errorMessage: String? = null,
    val clickThroughUrl: String? = null,
    val adBreakState: AdBreakState? = null,
)

private const val nextAdBreakMessageFormat = "Next ad break in %d s"
private const val currentAdBreakMessageFormat = "Current ad break ends in %d s"
private const val currentAdMessageFormat = "Playing ad %d / %d. Ends in %d s"
private const val adBreaksMessageFormat = "Ad Break %d: from: %d to: %d (%d ads)"
