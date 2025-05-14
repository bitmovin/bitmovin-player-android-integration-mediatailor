package com.bitmovin.player.integration.mediatailor

import android.util.Log
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.deficiency.SourceErrorCode
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.core.internal.extensionPoint
import com.bitmovin.player.integration.mediatailor.beaconing.DefaultMediaTailorAdBeaconing
import com.bitmovin.player.integration.mediatailor.beaconing.MediaTailorAdBeaconing
import com.bitmovin.player.integration.mediatailor.network.DefaultHttpClient
import com.bitmovin.player.integration.mediatailor.network.HttpClient
import com.bitmovin.player.integration.mediatailor.network.HttpRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "MediaTailorPlayer"

class MediaTailorPlayer(
    private val player: Player,
    private val eventEmitter: MediaTailorEventEmitter = MediaTailorEventEmitter(
        player.extensionPoint.eventEmitter
    ),
) : Player by player {
    private val httpClient = DefaultHttpClient()
    private val adMapper = DefaultMediaTailorAdsMapper()
    private val mediaTailorSession: MediaTailorSession = DefaultMediaTailorSession(
        httpClient = httpClient,
        adsMapper = adMapper,
    )
    private val adPlaybackTracker: MediaTailorAdPlaybackTracker =
        DefaultMediaTailorAdPlaybackTracker(
            player = player,
            mediaTailorSession = mediaTailorSession,
        )
    private val adBeaconing: MediaTailorAdBeaconing = DefaultMediaTailorAdBeaconing(
        player = player,
        adPlaybackTracker = adPlaybackTracker,
        httpClient = httpClient,
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
        refreshTrackingResponseJob?.cancel()
        refreshTrackingResponseJob = null
    }

    init {
        scope.launch {
            adPlaybackTracker.currentAdBreak.collect { adBreak ->
                Log.i(TAG, "Ad break: $adBreak")
            }
        }
        scope.launch {
            adPlaybackTracker.nextAdBreak.collect { nextAdBreak ->
                Log.i(TAG, "Next ad break: $nextAdBreak")
            }
        }
        scope.launch {
            adPlaybackTracker.adProgress.collect { ad ->
                Log.i(TAG, "Ad: $ad")
            }
        }
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

    override fun mute() {
        adBeaconing.track("mute")
        player.mute()
    }

    override fun unmute() {
        adBeaconing.track("unmute")
        player.unmute()
    }

    override fun play() {
        adBeaconing.track("play")
        player.play()
    }

    override fun pause() {
        adBeaconing.track("pause")
        player.pause()
    }

    private fun continuouslyFetchTrackingDataJob() = scope.launch {
        while (isActive) {
            if (isPlaying) {
                mediaTailorSession.fetchTrackingData()
            }
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
        adBeaconing.dispose()
        mediaTailorSession.dispose()
        refreshTrackingResponseJob?.cancel()
        refreshTrackingResponseJob = null
        adPlaybackTracker.dispose()
        scope.cancel()
        player.destroy()
    }
}
