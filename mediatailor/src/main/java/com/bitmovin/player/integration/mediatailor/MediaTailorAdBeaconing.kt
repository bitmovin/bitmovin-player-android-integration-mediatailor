package com.bitmovin.player.integration.mediatailor

import android.util.Log
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.integration.mediatailor.util.Disposable
import com.bitmovin.player.integration.mediatailor.model.MediaTailorTrackingEvent
import com.bitmovin.player.integration.mediatailor.network.HttpClient
import com.bitmovin.player.integration.mediatailor.util.eventFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val TAG = "AdBeaconing"

interface MediaTailorAdBeaconing : Disposable {
    fun track(eventType: String)
}

internal class DefaultMediaTailorAdBeaconing(
    val player: Player,
    val adPlaybackTracker: MediaTailorAdPlaybackTracker,
    val httpClient: HttpClient,
) : MediaTailorAdBeaconing {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val firedTrackingEvents = mutableSetOf<String>()

    init {
        scope.launch {
            adPlaybackTracker.adProgress.collect { adProgress ->
                val ad = adProgress?.ad ?: return@collect

                ad.trackingEvents
                    .filter { player.currentTime in it.paddedStartTime }
                    .filter { it.isLinearAdMetric }
                    .forEach {
                        if (firedTrackingEvents.contains(it.id)) {
                            return@forEach
                        }
                        firedTrackingEvents.add(it.id)

                        Log.d(TAG, "Tracking event: ${it.eventType}")
                        it.beaconUrls.forEach { launch { httpClient.get(it) } }
                    }
            }
        }
        scope.launch {
            combine(
                player.eventFlow<PlayerEvent.Muted>(),
                player.eventFlow<PlayerEvent.Unmuted>(),
            ) {
            }
        }
    }

    override fun track(eventType: String) {
        trackIfPresent(eventType)
    }

    private fun trackIfPresent(eventType: String) {
        val ad = adPlaybackTracker.adProgress.value?.ad ?: return

        ad.trackingEvents
            .find { it.eventType == eventType }
            ?.also {
                Log.d(TAG, "Tracking event: $eventType")
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
    get() = linearAdMetricEventTypes.contains(eventType)

// Player updates time every 0.2 seconds or so, so we need to account for this inaccuracy
// when checking if the tracking event should be fired
private val MediaTailorTrackingEvent.paddedStartTime: ClosedRange<Double>
    get() = startTime - 0.3..startTime + 0.3

private val linearAdMetricEventTypes = setOf(
    "loaded",
    "start",
    "firstQuartile",
    "midpoint",
    "thirdQuartile",
    "complete",
    "progress",
)