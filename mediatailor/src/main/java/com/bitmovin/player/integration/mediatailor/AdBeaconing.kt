package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.integration.mediatailor.api.LinearAdTrackingEvents
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorTrackingEvent
import com.bitmovin.player.integration.mediatailor.api.PlayerTrackingEvents
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
            player.eventFlow<PlayerEvent.TimeChanged>().collect { timeChangedEvent ->
                val ad = adPlaybackTracker.playingAdBreak.value?.ad ?: return@collect

                ad.trackingEvents
                    .filter { timeChangedEvent.time in it.paddedStartTime }
                    .filter { it.isLinearAdMetric }
                    .forEach { trackingEvent ->
                        if (trackingEvent.id in firedTrackingEvents) {
                            return@forEach
                        }
                        firedTrackingEvents.add(trackingEvent.id)

                        trackingEvent.beaconUrls.forEach {
                            eventEmitter.emit(MediaTailorEvent.Info("Tracking event '${trackingEvent.eventType}': $it"))
                            launch { httpClient.get(it) }
                        }
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
                    track(trackingEvent.eventType)
                }
            }
        }
    }

    override fun track(eventType: String) {
        val ad = adPlaybackTracker.playingAdBreak.value?.ad ?: return

        ad.trackingEvents
            .find { it.eventType == eventType }
            ?.beaconUrls
            ?.forEach {
                eventEmitter.emit(MediaTailorEvent.Info("Tracking event '$eventType': $it"))
                scope.launch { httpClient.get(it) }
            }
    }

    override fun dispose() {
        scope.cancel()
    }
}

private val MediaTailorTrackingEvent.isLinearAdMetric: Boolean
    get() = eventType in linearAdEventTypes

// Player updates time every 0.2 seconds or so, so we need to account for this inaccuracy
// when checking if the tracking event should be fired
private val MediaTailorTrackingEvent.paddedStartTime: ClosedRange<Double>
    get() = scheduleTime - 0.3..scheduleTime + 0.3

private val linearAdEventTypes = LinearAdTrackingEvents.values
    .map { it.eventType }
    .toSet()

private val playerToTrackingEvents = mapOf(
    PlayerEvent.Muted::class to PlayerTrackingEvents.Mute,
    PlayerEvent.Unmuted::class to PlayerTrackingEvents.Unmute,
    PlayerEvent.Play::class to PlayerTrackingEvents.Resume,
    PlayerEvent.Paused::class to PlayerTrackingEvents.Pause,
    PlayerEvent.FullscreenEnter::class to PlayerTrackingEvents.Fullscreen,
    PlayerEvent.FullscreenExit::class to PlayerTrackingEvents.ExitFullscreen,
)
