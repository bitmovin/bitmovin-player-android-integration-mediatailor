package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorTrackingEvent
import com.bitmovin.player.integration.mediatailor.eventEmitter.InternalEventEmitter
import com.bitmovin.player.integration.mediatailor.network.HttpClient
import com.bitmovin.player.integration.mediatailor.util.Disposable
import com.bitmovin.player.integration.mediatailor.util.eventFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

internal interface AdBeaconing : Disposable {
    fun track(eventType: String)
}

internal class DefaultAdBeaconing(
    val player: Player,
    val adPlaybackTracker: AdPlaybackTracker,
    val httpClient: HttpClient,
    val eventEmitter: InternalEventEmitter,
) : AdBeaconing {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val firedTrackingEvents = mutableSetOf<String>()

    init {
        scope.launch {
            player.eventFlow<PlayerEvent.TimeChanged>().collect {
                val ad = adPlaybackTracker.playingAdBreak.value?.ad ?: return@collect

                // TODO: Check if it's ok track E.g. third quartile if we skipped start and midpoint
                ad.trackingEvents
                    .filter { player.currentTime in it.paddedStartTime }
                    .filter { it.isLinearAdMetric }
                    .forEach { trackingEvent ->
                        if (trackingEvent.id in firedTrackingEvents) {
                            return@forEach
                        }
                        firedTrackingEvents.add(trackingEvent.id)

                        eventEmitter.emit(MediaTailorEvent.Info("Tracking event: ${trackingEvent.eventType}"))
                        trackingEvent.beaconUrls.forEach { launch { httpClient.get(it) } }
                    }
            }
        }
        scope.launch {
            merge(
                player.eventFlow<PlayerEvent.Muted>(),
                player.eventFlow<PlayerEvent.Unmuted>(),
                player.eventFlow<PlayerEvent.Play>(),
                player.eventFlow<PlayerEvent.Paused>(),
                player.eventFlow<PlayerEvent.FullscreenEnter>(),
                player.eventFlow<PlayerEvent.FullscreenExit>(),
            ).collect { event ->
                val trackingEvent = playerToTrackingEvents[event::class]
                if (trackingEvent != null) {
                    track(trackingEvent)
                }
            }
        }
    }

    override fun track(eventType: String) {
        val ad = adPlaybackTracker.playingAdBreak.value?.ad ?: return

        ad.trackingEvents
            .find { it.eventType == eventType }
            ?.also {
                eventEmitter.emit(MediaTailorEvent.Info("Tracking event: $eventType"))
            }
            ?.beaconUrls
            ?.forEach {
                scope.launch { httpClient.get(it) }
            }
    }

    override fun dispose() {
        scope.cancel()
    }
}

private val MediaTailorTrackingEvent.isLinearAdMetric: Boolean
    get() = eventType in linearAdMetricEventTypes

// Player updates time every 0.2 seconds or so, so we need to account for this inaccuracy
// when checking if the tracking event should be fired
private val MediaTailorTrackingEvent.paddedStartTime: ClosedRange<Double>
    get() = scheduleTime - 0.3..scheduleTime + 0.3

private val linearAdMetricEventTypes = setOf(
    "loaded",
    "start",
    "firstQuartile",
    "midpoint",
    "thirdQuartile",
    "complete",
    "progress",
    "impression",
)

private val playerToTrackingEvents = mapOf(
    PlayerEvent.Muted::class to "mute",
    PlayerEvent.Unmuted::class to "unmute",
    PlayerEvent.Play::class to "resume",
    PlayerEvent.Paused::class to "pause",
    PlayerEvent.FullscreenEnter::class to "fullscreen",
    PlayerEvent.FullscreenExit::class to "exitFullscreen",
)
