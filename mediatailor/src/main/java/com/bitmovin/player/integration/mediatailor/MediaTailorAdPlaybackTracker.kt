package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.core.internal.InternalEventEmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal interface MediaTailorAdPlaybackTracker : Disposable {

}

internal class DefaultMediaTailorAdPlaybackTracker(
    private val player: Player,
    private val mediaTailorSession: MediaTailorSession,
    private val eventEmitter: InternalEventEmitter<Event>,
) : MediaTailorAdPlaybackTracker {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _currentAdBreak = MutableStateFlow<MediaTailorAdBreak?>(null)
    private val _currentAd = MutableStateFlow<MediaTailorLinearAd?>(null)

    private val onTimeChanged: (PlayerEvent.TimeChanged) -> Unit = { event ->
        val previousAdBreak = _currentAdBreak.value
        if (previousAdBreak == null || previousAdBreak.isPassed == true) {
            _currentAdBreak.update { findCurrentAdBreak() }
        }

        val newAdBreak = _currentAdBreak.value
        if (newAdBreak != null) {
            val currentAd = _currentAd.value
            if (currentAd == null || currentAd.isPassed == true) {
                _currentAd.update { newAdBreak.findCurrentAd() }
            }
        } else {
            _currentAd.update { null }
        }
    }

    init {
        player.on<PlayerEvent.TimeChanged>(onTimeChanged)
        scope.launch {
            _currentAdBreak.collect { adBreak ->
                if (adBreak != null) {
                    eventEmitter.emit(PlayerEvent.AdBreakStarted())
                } else {
                    eventEmitter.emit(PlayerEvent.AdBreakFinished())
                }
            }
        }
        scope.launch {
            _currentAd.collect { ad ->
                if (ad != null) {
                    eventEmitter.emit(
                        PlayerEvent.AdStarted(
                            clientType = AdSourceType.Bitmovin,
                            clickThroughUrl = null,
                            indexInQueue = 0, // TODO,
                            duration = ad.duration,
                            timeOffset = ad.scheduleTime,
                            position = null, // TODO,
                            skipOffset = 0.0,
                            ad = null,
                        )
                    )
                } else {
                    eventEmitter.emit(PlayerEvent.AdFinished(ad))
                }
            }
        }
    }

    private fun findCurrentAdBreak(): MediaTailorAdBreak? {
        return mediaTailorSession.adBreaks.find {
            player.currentTime in (it.scheduleTime..it.scheduleTime + it.duration)
        }
    }

    private fun MediaTailorAdBreak.findCurrentAd(): MediaTailorLinearAd? {
        return ads.find {
            player.currentTime in (it.scheduleTime..it.scheduleTime + it.duration)
        }
    }

    private val MediaTailorAdBreak.isPassed: Boolean get() = endTime < player.currentTime

    private val MediaTailorLinearAd.isPassed: Boolean get() = endTime < player.currentTime

    private val MediaTailorAdBreak.endTime: Double get() = scheduleTime + duration

    private val MediaTailorLinearAd.endTime: Double get() = scheduleTime + duration

    override fun dispose() {
        scope.cancel()
        player.off(onTimeChanged)
    }
}
