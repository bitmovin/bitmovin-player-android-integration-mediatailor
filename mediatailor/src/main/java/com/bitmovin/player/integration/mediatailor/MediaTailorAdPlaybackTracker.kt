package com.bitmovin.player.integration.mediatailor

import android.util.Log
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.integration.mediatailor.api.AdProgress
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.api.MediaTailorLinearAd
import com.bitmovin.player.integration.mediatailor.util.Disposable
import com.bitmovin.player.integration.mediatailor.util.eventFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "AdPlaybackTracker"

internal interface MediaTailorAdPlaybackTracker : Disposable {
    val nextAdBreak: StateFlow<MediaTailorAdBreak?>
    val currentAdBreak: StateFlow<MediaTailorAdBreak?>
    val adProgress: StateFlow<AdProgress?>
}

internal class DefaultMediaTailorAdPlaybackTracker(
    private val player: Player,
    private val mediaTailorSession: MediaTailorSession,
) : MediaTailorAdPlaybackTracker {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _nextAdBreak = MutableStateFlow<MediaTailorAdBreak?>(null)
    private val _currentAdBreak = MutableStateFlow<MediaTailorAdBreak?>(null)
    private val _currentAd = MutableStateFlow<MediaTailorLinearAd?>(null)
    private val _adProgress = MutableStateFlow<AdProgress?>(null)

    override val nextAdBreak: StateFlow<MediaTailorAdBreak?>
        get() = _nextAdBreak

    override val currentAdBreak: StateFlow<MediaTailorAdBreak?>
        get() = _currentAdBreak

    override val adProgress: StateFlow<AdProgress?>
        get() = _adProgress

    init {
        findAdBreaksIfNeeded()
        scope.launch {
            player.eventFlow<PlayerEvent.Seeked>().collect {
                _nextAdBreak.update { null }
                _currentAdBreak.update { null }
                findAdBreaksIfNeeded()
            }
        }
        scope.launch {
            mediaTailorSession.adBreaks.collect { adBreaks ->
                findAdBreaksIfNeeded()
            }
        }
        scope.launch {
            player.eventFlow<PlayerEvent.TimeChanged>().collect {
                trackAdBreaks()
                _currentAdBreak.value.updateCurrentAdIfNeeded()
                _currentAd.value.trackAdPlayback()
            }
        }
    }

    /**
     * Next ad break makes sense to update only every time the ad breaks is updated
     */
    private fun findAdBreaksIfNeeded() {
        val nextAdBreak = _nextAdBreak.value
        when {
            nextAdBreak == null -> {
                val nextAdBreak = findNextAdBreak()
                if (nextAdBreak == null) {
                    // We might have joined the stream in the middle of an ad break
                    // and there is no next ad break yet.
                    _currentAdBreak.update { findCurrentAdBreak() }
                } else {
                    // Set the next ad break
                    _nextAdBreak.update { nextAdBreak }
                }
            }
        }
    }

    /**
     * To avoid searching for current ad break on every player time update we track the next
     * ad break and then continuously check if we reached it yet.
     */
    private fun trackAdBreaks() {
        val nextAdBreak = _nextAdBreak.value
        val currentAdBreak = _currentAdBreak.value
        when {
            // We started playing the ad break that was upcoming before
            nextAdBreak != null && player.currentTime in nextAdBreak.startToEndTime -> {
                // Next ad break became current ad break
                _currentAdBreak.update { nextAdBreak }
                // Reset next ad break
                _nextAdBreak.update { null }
            }

            // We passed the next ad break completely
            nextAdBreak != null && nextAdBreak.endTime < player.currentTime -> {
                // We passed the next ad break
                _nextAdBreak.update { null }
            }

            // We passed the current ad break
            currentAdBreak != null && currentAdBreak.endTime < player.currentTime -> {
                // We finished the current ad break
                _currentAdBreak.update { null }
            }
        }
    }

    private fun MediaTailorAdBreak?.updateCurrentAdIfNeeded() {
        val currentAdBreak = this
        if (currentAdBreak != null) {
            val currentAd = _currentAd.value
            if (currentAd == null || player.currentTime !in currentAd.startToEndTime) {
                _currentAd.update { currentAdBreak.findCurrentAd() }
            }
        } else {
            _currentAd.update { null }
            _adProgress.update { null }
        }
    }

    private fun MediaTailorLinearAd?.trackAdPlayback() {
        val currentAd = this ?: return

        val currentAdTime = player.currentTime - currentAd.scheduleTime
        val progress = currentAdTime / currentAd.duration

        _adProgress.update {
            AdProgress(
                ad = currentAd,
                progress = progress,
            )
        }

        Log.i(
            TAG,
            "Ad progress: $progress, currentAdTime: $currentAdTime"
        )
    }

    private fun findNextAdBreak(): MediaTailorAdBreak? = mediaTailorSession.adBreaks.value.find {
        it.scheduleTime > player.currentTime
    }

    private fun findCurrentAdBreak(): MediaTailorAdBreak? = mediaTailorSession.adBreaks.value.find {
        player.currentTime in it.startToEndTime
    }

    private fun MediaTailorAdBreak.findCurrentAd(): MediaTailorLinearAd? = ads.find {
        player.currentTime in it.startToEndTime
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
