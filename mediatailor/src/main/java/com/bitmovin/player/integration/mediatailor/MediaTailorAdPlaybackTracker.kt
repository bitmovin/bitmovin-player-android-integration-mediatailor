package com.bitmovin.player.integration.mediatailor

import android.util.Log
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.integration.mediatailor.util.eventFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "AdPlaybackTracker"

public data class AdProgress(
    val adBreak: MediaTailorAdBreak,
    val ad: MediaTailorLinearAd,
    val progress: Double,
)

interface MediaTailorAdPlaybackTracker : Disposable {
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
        scope.launch {
            mediaTailorSession.adBreaks.collect { adBreaks ->
                updateNextAdBreak()
            }
        }
        scope.launch {
            player.eventFlow<PlayerEvent.TimeChanged>().collect {
                updateNextAndCurrentAdBreaksIfNeeded()
                updateCurrentAdIfNeeded()
                trackAdPlayback()
            }
        }
    }

    /**
     * Next ad break makes sense to update only every time the ad breaks is updated
     */
    private fun updateNextAdBreak() {
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
    private fun updateNextAndCurrentAdBreaksIfNeeded() {
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
            currentAdBreak != null && player.currentTime > currentAdBreak.endTime -> {
                // We finished the current ad break
                _currentAdBreak.update { null }
            }
        }
    }

    /**
     * Current ad is always based on current ad break.
     */
    private fun updateCurrentAdIfNeeded() {
        val currentAdBreak = _currentAdBreak.value
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

    private fun trackAdPlayback() {
        val currentAdBreak = _currentAdBreak.value ?: return
        val currentAd = _currentAd.value ?: return

        val currentAdTime = player.currentTime - currentAd.scheduleTime
        val progress = currentAdTime / currentAd.duration

        _adProgress.update {
            AdProgress(
                adBreak = currentAdBreak,
                ad = currentAd,
                progress = progress,
            )
        }

        Log.i(
            TAG,
            "Ad progress: $progress, currentAdTime: $currentAdTime, duration: ${currentAdBreak.duration}"
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
