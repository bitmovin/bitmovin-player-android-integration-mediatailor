package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.eventEmitter.InternalEventEmitter
import com.bitmovin.player.integration.mediatailor.util.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal interface AdPlaybackEventEmitter : Disposable

internal class DefaultAdPlaybackEventEmitter(
    private val adPlaybackTracker: AdPlaybackTracker,
    private val eventEmitter: InternalEventEmitter,
) : AdPlaybackEventEmitter {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var previousPlayingAdBreak: PlayingAdBreak? = null

    init {
        scope.launch {
            adPlaybackTracker.nextAdBreak.collect {
                eventEmitter.emit(MediaTailorEvent.UpcomingAdBreakUpdated(it))
            }
        }
        scope.launch {
            adPlaybackTracker.playingAdBreak.collect { playingAdBreak ->
                val previousPlayingAdBreak = previousPlayingAdBreak
                when {
                    previousPlayingAdBreak == null && playingAdBreak != null -> {
                        eventEmitter.emit(MediaTailorEvent.AdBreakStarted(playingAdBreak.adBreak))
                        eventEmitter.emit(
                            MediaTailorEvent.AdStarted(
                                ad = playingAdBreak.ad,
                                indexInQueue = playingAdBreak.adIndex,
                            ),
                        )
                    }

                    previousPlayingAdBreak != null && playingAdBreak != null &&
                        previousPlayingAdBreak.adBreak.id != playingAdBreak.adBreak.id -> {
                        eventEmitter.emit(MediaTailorEvent.AdFinished(previousPlayingAdBreak.ad))
                        eventEmitter.emit(MediaTailorEvent.AdBreakFinished(previousPlayingAdBreak.adBreak))
                        eventEmitter.emit(MediaTailorEvent.AdBreakStarted(playingAdBreak.adBreak))
                        eventEmitter.emit(
                            MediaTailorEvent.AdStarted(
                                ad = playingAdBreak.ad,
                                indexInQueue = playingAdBreak.adIndex,
                            ),
                        )
                    }

                    playingAdBreak != null && previousPlayingAdBreak?.ad?.id != playingAdBreak.ad.id -> {
                        if (previousPlayingAdBreak?.ad != null) {
                            eventEmitter.emit(MediaTailorEvent.AdFinished(previousPlayingAdBreak.ad))
                        }
                        eventEmitter.emit(
                            MediaTailorEvent.AdStarted(
                                ad = playingAdBreak.ad,
                                indexInQueue = playingAdBreak.adIndex,
                            ),
                        )
                    }

                    previousPlayingAdBreak != null && playingAdBreak == null -> {
                        eventEmitter.emit(MediaTailorEvent.AdFinished(previousPlayingAdBreak.ad))
                        eventEmitter.emit(MediaTailorEvent.AdBreakFinished(previousPlayingAdBreak.adBreak))
                    }
                }

                this@DefaultAdPlaybackEventEmitter.previousPlayingAdBreak = playingAdBreak
            }
        }
    }

    override fun dispose() {
        scope.cancel()
    }
}
