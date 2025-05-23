package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.api.MediaTailorLinearAd
import com.bitmovin.player.integration.mediatailor.util.Disposable
import com.bitmovin.player.integration.mediatailor.util.eventFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PlayingAdBreak(
    val adBreak: MediaTailorAdBreak,
    val adIndex: Int,
) {
    val ad: MediaTailorLinearAd
        get() = adBreak.ads[adIndex]
}

internal interface AdPlaybackTracker : Disposable {
    val nextAdBreak: StateFlow<MediaTailorAdBreak?>
    val playingAdBreak: StateFlow<PlayingAdBreak?>
}

internal class DefaultAdPlaybackTracker(
    private val player: Player,
    private val mediaTailorSession: MediaTailorSession,
) : AdPlaybackTracker {
    private val _nextAdBreak = MutableStateFlow<MediaTailorAdBreak?>(null)
    private val _playingAdBreak = MutableStateFlow<PlayingAdBreak?>(null)

    override val nextAdBreak: StateFlow<MediaTailorAdBreak?>
        get() = _nextAdBreak
    override val playingAdBreak: StateFlow<PlayingAdBreak?>
        get() = _playingAdBreak

    private var currentAdBreakIndex: Int = 0
    private var currentAdIndex: Int = 0

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            player.eventFlow<PlayerEvent.TimeChanged>().collect {
                trackAdBreaks()
            }
        }
        scope.launch {
            merge(
                player.eventFlow<PlayerEvent.Seeked>(),
                player.eventFlow<PlayerEvent.TimeShifted>(),
            ).collect {
                resetState()
            }
        }
    }

    private fun resetState() {
        currentAdBreakIndex = 0
        currentAdIndex = 0

        _nextAdBreak.update { null }
        _playingAdBreak.update { null }
    }

    private fun trackAdBreaks() {
        // This function assumes that ad breaks are sorted by schedule time
        val adBreaks = mediaTailorSession.adBreaks.value.takeIf { it.isNotEmpty() } ?: return

        while (currentAdBreakIndex < adBreaks.lastIndex &&
            player.currentTime !in adBreaks[currentAdBreakIndex].startToEndTime
        ) {
            currentAdBreakIndex++
        }

        val adBreak = adBreaks[currentAdBreakIndex]

        if (player.currentTime < adBreak.scheduleTime) {
            _nextAdBreak.update { adBreak }
        } else {
            _nextAdBreak.update { null }
        }

        if (player.currentTime !in adBreak.startToEndTime) {
            _playingAdBreak.update { null }
            currentAdIndex = 0
            return
        }

        val ads = adBreak.ads
        while (currentAdIndex < ads.lastIndex &&
            player.currentTime !in ads[currentAdIndex].startToEndTime
        ) {
            currentAdIndex++
        }

        val ad = ads[currentAdIndex]
        if (player.currentTime in ad.startToEndTime) {
            _playingAdBreak.update {
                PlayingAdBreak(
                    adBreak = adBreak,
                    adIndex = currentAdIndex,
                )
            }
        }
    }

    override fun dispose() {
        scope.cancel()
    }
}

private val MediaTailorAdBreak.endTime: Double
    get() = scheduleTime + duration

private val MediaTailorAdBreak.startToEndTime: ClosedRange<Double>
    get() = scheduleTime..endTime

private val MediaTailorLinearAd.endTime: Double
    get() = scheduleTime + duration

private val MediaTailorLinearAd.startToEndTime: ClosedRange<Double>
    get() = scheduleTime..endTime
