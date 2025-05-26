package com.bitmovin.player.integration.mediatailor.api

import com.bitmovin.player.api.Player
import com.bitmovin.player.integration.mediatailor.AdBeaconing
import com.bitmovin.player.integration.mediatailor.AdPlaybackEventEmitter
import com.bitmovin.player.integration.mediatailor.AdPlaybackTracker
import com.bitmovin.player.integration.mediatailor.DefaultAdBeaconing
import com.bitmovin.player.integration.mediatailor.DefaultAdPlaybackEventEmitter
import com.bitmovin.player.integration.mediatailor.DefaultAdPlaybackTracker
import com.bitmovin.player.integration.mediatailor.DefaultAdsMapper
import com.bitmovin.player.integration.mediatailor.DefaultMediaTailorSession
import com.bitmovin.player.integration.mediatailor.MediaTailorSession
import com.bitmovin.player.integration.mediatailor.eventEmitter.FlowEventEmitter
import com.bitmovin.player.integration.mediatailor.network.DefaultHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * TODO: Docs
 * Limitations: - Playlists,...
 */
public class MediaTailorSessionManager(
    private val player: Player,
) {
    private val httpClient = DefaultHttpClient()
    private val adMapper = DefaultAdsMapper()
    private val flowEventEmitter = FlowEventEmitter()
    private var session: MediaTailorSession? = null
    private var adPlaybackTracker: AdPlaybackTracker? = null
    private var adPlaybackProcessor: AdPlaybackEventEmitter? = null
    private var adBeaconing: AdBeaconing? = null

    public val events: Flow<MediaTailorEvent>
        get() = flowEventEmitter.events

    public suspend fun initializeSession(
        sessionConfig: MediaTailorSessionConfig
    ): SessionInitializationResult = withContext(Dispatchers.Main) {
        if (session != null) {
            val message =
                "Session already initialized. Stop the previous session before initializing a new one."
            flowEventEmitter.emit(MediaTailorEvent.Error(message))
            return@withContext SessionInitializationResult.Failure(message)
        }
        val session = DefaultMediaTailorSession(
            player,
            httpClient,
            adMapper,
            sessionConfig,
        )
        val sessionInitResult = session.initialize(sessionConfig)

        if (sessionInitResult is SessionInitializationResult.Success) {
            adPlaybackTracker = DefaultAdPlaybackTracker(
                player,
                session,
            )
            adPlaybackProcessor = DefaultAdPlaybackEventEmitter(
                adPlaybackTracker!!,
                flowEventEmitter,
            )
            adBeaconing = DefaultAdBeaconing(
                player,
                adPlaybackTracker!!,
                httpClient,
                flowEventEmitter,
            )
            this@MediaTailorSessionManager.session = session
        }

        sessionInitResult
    }

    public fun stopSession() {
        adPlaybackTracker?.dispose()
        adPlaybackTracker = null
        adBeaconing?.dispose()
        adBeaconing = null
        session?.dispose()
        session = null
    }
}
