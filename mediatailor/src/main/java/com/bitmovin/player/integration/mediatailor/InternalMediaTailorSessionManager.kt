package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionManager
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult
import com.bitmovin.player.integration.mediatailor.api.TrackingEvent
import com.bitmovin.player.integration.mediatailor.eventEmitter.asFlowEventEmitter
import com.bitmovin.player.integration.mediatailor.util.DependencyFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class InternalMediaTailorSessionManager(
    private val player: Player,
    private val dependencyFactory: DependencyFactory = DependencyFactory(),
) : MediaTailorSessionManager {
    private val sessionManagerScope = dependencyFactory.createScope(
        Dispatchers.Main + SupervisorJob(),
    )
    private val httpClient = dependencyFactory.createHttpClient()
    private val adMapper = dependencyFactory.createAdsMapper()
    private val eventEmitter = dependencyFactory.createEventEmitter()
    private var session: MediaTailorSession? = null
    private var adPlaybackTracker: AdPlaybackTracker? = null
    private var adPlaybackEventEmitter: AdPlaybackEventEmitter? = null
    private var mediaTailorSessionEventEmitter: MediaTailorSessionEventEmitter? = null
    private var adBeaconing: AdBeaconing? = null

    override val events: Flow<MediaTailorEvent>
        get() = eventEmitter.asFlowEventEmitter().events

    override suspend fun initializeSession(sessionConfig: MediaTailorSessionConfig): SessionInitializationResult =
        withContext(Dispatchers.Main) {
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
                    eventEmitter,
                )
                mediaTailorSessionEventEmitter = dependencyFactory.createMediaTailorSessionEventEmitter(
                    session,
                    eventEmitter,
                )
                adBeaconing = dependencyFactory.createAdBeaconing(
                    player,
                    adPlaybackTracker!!,
                    httpClient,
                    eventEmitter,
                    sessionConfig,
                )
                this@InternalMediaTailorSessionManager.session = session
            }

            sessionInitResult
        }

    override fun sendTrackingEvent(event: TrackingEvent) {
        val adBeaconing = adBeaconing
        if (adBeaconing == null) {
            sessionManagerScope.launch {
                eventEmitter.emit(MediaTailorEvent.Error("Cannot send tracking events before session is initialized."))
            }
            return
        }
        adBeaconing.track(event.eventType)
    }

    override fun stopSession() {
        adPlaybackTracker?.dispose()
        adPlaybackTracker = null
        adPlaybackEventEmitter?.dispose()
        adPlaybackEventEmitter = null
        mediaTailorSessionEventEmitter?.dispose()
        mediaTailorSessionEventEmitter = null
        adBeaconing?.dispose()
        adBeaconing = null
        session?.dispose()
        session = null
    }

    override fun destroy() {
        stopSession()
        sessionManagerScope.cancel()
    }
}
