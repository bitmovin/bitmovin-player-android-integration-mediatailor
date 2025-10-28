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
import kotlinx.coroutines.flow.collectLatest
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
            stateShouldBeInvalidatedFlow().collectLatest {
                currentAdBreakIndex = 0
                currentAdIndex = 0
                val adBreaks = mediaTailorSession.adBreaks.value

                player.eventFlow<PlayerEvent.TimeChanged>().collect {
                    trackAdBreaks(adBreaks)
                }
            }
        }
    }

    private fun stateShouldBeInvalidatedFlow() = merge(
        // When seeking or time shifting, the calculated ad breaks are most likely outdated
        player.eventFlow<PlayerEvent.Seeked>(),
        player.eventFlow<PlayerEvent.TimeShifted>(),
        // In case ad breaks are updated before the tail the indices might point to an invalid ad break
        mediaTailorSession.adBreaks,
    )

    /**
     * This function uses [currentAdBreakIndex] and [currentAdIndex] to track the current position
     * of the [playingAdBreak] and [nextAdBreak] to avoid unnecessary iterations over all ad breaks.
     *
     * It assumes that ad breaks are sorted by schedule time.
     */
    private fun trackAdBreaks(adBreaks: List<MediaTailorAdBreak>) {
        if (adBreaks.isEmpty()) {
            return
        }

        while (currentAdBreakIndex < adBreaks.lastIndex &&
            player.currentTime >= adBreaks[currentAdBreakIndex].endTime
        ) {
            currentAdBreakIndex++
            currentAdIndex = 0
        }

        val adBreak = adBreaks[currentAdBreakIndex]
        when {
            player.currentTime < adBreak.scheduleTime -> {
                _nextAdBreak.update { adBreak }
            }

            adBreaks.getOrNull(currentAdBreakIndex + 1) != null -> {
                _nextAdBreak.update { adBreaks[currentAdBreakIndex + 1] }
            }

            else -> {
                _nextAdBreak.update { null }
            }
        }

        if (player.currentTime !in adBreak.startToEndTime) {
            _playingAdBreak.update { null }
            return
        }

        val ads = adBreak.ads
        // No need to track if no ads in the adbreak
        if(ads.isEmpty()) {
            return
        }

        while (currentAdIndex < ads.lastIndex &&
            player.currentTime >= ads[currentAdIndex].endTime
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
