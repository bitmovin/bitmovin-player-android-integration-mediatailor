package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionManager
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult
import com.bitmovin.player.integration.mediatailor.util.DependencyFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class DefaultMediaTailorSessionManager(
    private val player: Player,
    private val dependencyFactory: DependencyFactory = DependencyFactory(),
) : MediaTailorSessionManager {
    private val httpClient = dependencyFactory.createHttpClient()
    private val adMapper = dependencyFactory.createAdsMapper()
    private val flowEventEmitter = dependencyFactory.createEventEmitter()
    private var session: MediaTailorSession? = null
    private var adPlaybackTracker: AdPlaybackTracker? = null
    private var adPlaybackEventEmitter: AdPlaybackEventEmitter? = null
    private var adBeaconing: AdBeaconing? = null

    override val events: Flow<MediaTailorEvent>
        get() = flowEventEmitter.events

    override suspend fun initializeSession(
        sessionConfig: MediaTailorSessionConfig
    ): SessionInitializationResult = withContext(Dispatchers.Main) {
        if (session != null) {
            val message =
                "Session already initialized. Stop the previous session before initializing a new one."
            return@withContext SessionInitializationResult.Failure(message)
        }
        val session = dependencyFactory.createMediaTailorSession(
            player,
            httpClient,
            adMapper,
        )
        val sessionInitResult = session.initialize(sessionConfig)

        if (sessionInitResult is SessionInitializationResult.Success) {
            adPlaybackTracker = dependencyFactory.createAdPlaybackTracker(
                player,
                session,
            )
            adPlaybackEventEmitter = dependencyFactory.createAdPlaybackEventEmitter(
                adPlaybackTracker!!,
                flowEventEmitter,
            )
            adBeaconing = dependencyFactory.createAdBeaconing(
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
        adPlaybackEventEmitter?.dispose()
        adPlaybackEventEmitter = null
        adBeaconing?.dispose()
        adBeaconing = null
        session?.dispose()
        session = null
    }
}
