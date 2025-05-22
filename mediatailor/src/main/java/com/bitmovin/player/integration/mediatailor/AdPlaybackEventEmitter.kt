package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorLinearAd
import com.bitmovin.player.integration.mediatailor.eventEmitter.InternalEventEmitter
import com.bitmovin.player.integration.mediatailor.util.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal interface AdPlaybackEventEmitter : Disposable

internal class DefaultAdPlaybackEventEmitter(
    private val adPlaybackTracker: MediaTailorAdPlaybackTracker,
    private val eventEmitter: InternalEventEmitter,
) : AdPlaybackEventEmitter {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var previousAdBreak: MediaTailorAdBreak? = null
    private var previousAd: MediaTailorLinearAd? = null

    init {
        scope.launch {
            adPlaybackTracker.currentAdBreak.collect { newAdBreak ->
                when {
                    previousAdBreak == null && newAdBreak != null -> {
                        eventEmitter.emit(MediaTailorEvent.AdBreakStarted(newAdBreak))
                    }

                    previousAdBreak != null && newAdBreak == null -> {
                        eventEmitter.emit(MediaTailorEvent.AdBreakFinished(previousAdBreak!!))
                    }

                    previousAdBreak != null && newAdBreak != null && previousAdBreak!!.id != newAdBreak.id -> {
                        eventEmitter.emit(MediaTailorEvent.AdBreakFinished(previousAdBreak!!))
                        eventEmitter.emit(MediaTailorEvent.AdBreakStarted(newAdBreak))
                    }
                }
                previousAdBreak = newAdBreak
            }
        }
        scope.launch {
            adPlaybackTracker.currentAd.collect { adProgress ->
                when {
                    previousAd == null && adProgress?.ad != null -> {
                        eventEmitter.emit(MediaTailorEvent.AdStarted(adProgress.ad))
                    }

                    previousAd != null && adProgress?.ad == null -> {
                        eventEmitter.emit(MediaTailorEvent.AdFinished(previousAd!!))
                    }

                    previousAd != null && adProgress?.ad != null && previousAd!!.id != adProgress.ad.id -> {
                        eventEmitter.emit(MediaTailorEvent.AdFinished(previousAd!!))
                        eventEmitter.emit(MediaTailorEvent.AdStarted(adProgress.ad))
                    }

                    adProgress != null -> {
                        eventEmitter.emit(
                            MediaTailorEvent.AdProgress(
                                adProgress.ad,
                                adProgress.progress
                            )
                        )
                    }
                }
                previousAd = adProgress?.ad
            }
        }
        scope.launch {
            adPlaybackTracker.nextAdBreak.collect { nextAdBreak ->
                eventEmitter.emit(MediaTailorEvent.UpcomingAdBreakUpdate(nextAdBreak))
            }
        }
    }

    override fun dispose() {
        previousAdBreak = null
        previousAd = null
        scope.cancel()
    }
}
