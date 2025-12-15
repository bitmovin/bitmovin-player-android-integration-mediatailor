package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.integration.mediatailor.api.LinearAdTrackingEvents
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorLinearAd
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
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
    val sessionConfig: MediaTailorSessionConfig,
) : AdBeaconing {
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        if (sessionConfig.automaticAdTrackingEnabled) {
            trackLinearAdMetrics()
            trackPlayerEvents()
        }
    }

    private fun trackLinearAdMetrics() = scope.launch {
        val firedTrackingIds = mutableSetOf<String>()
        var previousAdBreakId: String? = null
        player.eventFlow<PlayerEvent.TimeChanged>().collect { timeChangedEvent ->
            val playingAdBreak = adPlaybackTracker.playingAdBreak.value
            if (playingAdBreak?.adBreak?.id != previousAdBreakId) {
                previousAdBreakId = playingAdBreak?.adBreak?.id
                // We passed the ad break, we can clear the fired tracking events,
                // so that they can fire again in case the user re-enters the ad break
                firedTrackingIds.clear()
            }
            if (playingAdBreak == null) {
                return@collect
            }
            val adBreak = playingAdBreak.adBreak
            val ad = playingAdBreak.ad ?: return@collect

            ad.trackingEvents
                .filter { timeChangedEvent.time in it.paddedStartTime }
                .filter { it.isLinearAdMetric }
                .forEach { trackingEvent ->
                    val trackingId = trackingId(adBreak, ad, trackingEvent)
                    if (trackingId !in firedTrackingIds) {
                        firedTrackingIds.add(trackingId)
                        trackingEvent.beaconUrls.forEach {
                            val message = "Tracking event '${trackingEvent.eventType}': $it"
                            eventEmitter.emit(MediaTailorEvent.Info(message))
                            launch { httpClient.get(it) }
                        }
                    }
                }
        }
    }

    private fun trackPlayerEvents() = scope.launch {
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

    override fun track(eventType: String) {
        val ad = adPlaybackTracker.playingAdBreak.value?.ad ?: return

        ad.trackingEvents
            .find { it.eventType == eventType }
            ?.beaconUrls
            ?.forEach {
                scope.launch {
                    eventEmitter.emit(MediaTailorEvent.Info("Tracking event '$eventType': $it"))
                    httpClient.get(it)
                }
            }
    }

    override fun dispose() {
        scope.cancel()
    }
}

/**
 * The tracking event ids are not unique per event,
 * but are rather a sequence number for HLS and start time for DASH.
 * Reference:
 * https://docs.aws.amazon.com/mediatailor/latest/ug/ad-reporting-client-side-ad-tracking-schema.html
 *
 * This means that multiple events can have the same id (E.g. impression, start and first quartile),
 * if they are scheduled in the same segment.
 * To assure a unique tracking id, we combine all the available ids + event type.
 */
private fun trackingId(
    adBreak: MediaTailorAdBreak,
    ad: MediaTailorLinearAd,
    trackingEvent: MediaTailorTrackingEvent,
): String = "${adBreak.id}-${ad.id}-${trackingEvent.id}-${trackingEvent.eventType}"

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
