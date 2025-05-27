package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionManager
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult
import com.bitmovin.player.integration.mediatailor.eventEmitter.FlowEventEmitter
import com.bitmovin.player.integration.mediatailor.network.DefaultHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class DefaultMediaTailorSessionManager(
    private val player: Player,
) : MediaTailorSessionManager {
    private val httpClient = DefaultHttpClient()
    private val adMapper = DefaultAdsMapper()
    private val flowEventEmitter = FlowEventEmitter()
    private var session: MediaTailorSession? = null
    private var adPlaybackTracker: AdPlaybackTracker? = null
    private var adPlaybackProcessor: AdPlaybackEventEmitter? = null
    private var adBeaconing: AdBeaconing? = null

    override val events: Flow<MediaTailorEvent>
        get() = flowEventEmitter.events

    override suspend fun initializeSession(
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
            this@DefaultMediaTailorSessionManager.session = session
        }

        sessionInitResult
    }

    override fun stopSession() {
        adPlaybackTracker?.dispose()
        adPlaybackTracker = null
        adBeaconing?.dispose()
        adBeaconing = null
        session?.dispose()
        session = null
    }
}
