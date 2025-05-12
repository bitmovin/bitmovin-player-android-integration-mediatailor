package com.bitmovin.player.integration.mediatailor

import android.util.Log
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.deficiency.SourceErrorCode
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.core.internal.extensionPoint
import com.bitmovin.player.integration.mediatailor.network.DefaultHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MediaTailorPlayer(
    private val player: Player,
    private val eventEmitter: MediaTailorEventEmitter = MediaTailorEventEmitter(
        player.extensionPoint.eventEmitter
    ),
) : Player by player {
    private val mediaTailorSession: MediaTailorSession = DefaultMediaTailorSession(
        httpClient = DefaultHttpClient(),
        eventEmitter = eventEmitter,
        adsMapper = DefaultMediaTailorAdsMapper(),
    )
    private val adPlaybackTracker: MediaTailorAdPlaybackTracker =
        DefaultMediaTailorAdPlaybackTracker(
            player = player,
            mediaTailorSession = mediaTailorSession,
            eventEmitter = eventEmitter,
        )
    private val scope = CoroutineScope(Dispatchers.Main)
    private var refreshTrackingResponseJob: Job? = null

    private val onSourceLoaded: (SourceEvent.Loaded) -> Unit = { event ->
        if (player.isLive) {
            refreshTrackingResponseJob?.cancel()
            refreshTrackingResponseJob = continuouslyFetchTrackingDataJob()
        } else {
            scope.launch { mediaTailorSession.fetchTrackingData() }
        }
    }

    private val onSourceUnloaded: (SourceEvent.Unloaded) -> Unit = { event ->
        mediaTailorSession.dispose()
    }

    fun load(mediaTailorSourceConfig: MediaTailorSourceConfig) {
        scope.launch {
            val result = mediaTailorSession.initialize(mediaTailorSourceConfig)
            result.fold(
                onSuccess = {
                    Log.d("MediaTailorPlayer", "Session initialized successfully")
                    registerPlayerEvents()
                    player.load(it)
                },
                onFailure = {
                    Log.e("MediaTailorPlayer", "Failed to initialize session: ${it.message}")
                    eventEmitter.emit(
                        SourceEvent.Error(
                            code = SourceErrorCode.General,
                            message = "Failed to initialize the MediaTailor session: $mediaTailorSourceConfig"
                        )
                    )
                }
            )
        }
    }

    private fun continuouslyFetchTrackingDataJob() = scope.launch {
        while (isActive) {
            mediaTailorSession.fetchTrackingData()
            delay(4_000)
        }
    }

    private fun registerPlayerEvents() {
        player.on(onSourceLoaded)
        player.on(onSourceUnloaded)
    }

    private fun unregisterPlayerEvents() {
        player.off(onSourceLoaded)
        player.off(onSourceUnloaded)
    }

    override fun destroy() {
        unregisterPlayerEvents()
        adPlaybackTracker.dispose()
        mediaTailorSession.dispose()
        refreshTrackingResponseJob?.cancel()
        refreshTrackingResponseJob = null
        scope.cancel()
        player.destroy()
    }
}
