package com.bitmovin.player.integration.mediatailor.api

import com.bitmovin.player.api.Player
import com.bitmovin.player.integration.mediatailor.DefaultMediaTailorAdBeaconing
import com.bitmovin.player.integration.mediatailor.DefaultMediaTailorAdPlaybackTracker
import com.bitmovin.player.integration.mediatailor.DefaultMediaTailorAdsMapper
import com.bitmovin.player.integration.mediatailor.DefaultMediaTailorSession
import com.bitmovin.player.integration.mediatailor.MediaTailorAdBeaconing
import com.bitmovin.player.integration.mediatailor.MediaTailorAdPlaybackTracker
import com.bitmovin.player.integration.mediatailor.MediaTailorSession
import com.bitmovin.player.integration.mediatailor.network.DefaultHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * TODO: Docs
 * Limitations: - Playlists,...
 */
public class MediaTailorSessionManager(
    val player: Player,
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val httpClient = DefaultHttpClient()
    private val adMapper = DefaultMediaTailorAdsMapper()
    private var session: MediaTailorSession? = null
    private var adPlaybackTracker: MediaTailorAdPlaybackTracker? = null
    private var adBeaconing: MediaTailorAdBeaconing? = null

    // TODO: Maybe remove this
    public fun initializeSession(
        sessionConfig: MediaTailorSessionConfig,
        callback: (SessionInitializationResult) -> Unit
    ) {
        scope.launch {
            val sessionResult = initializeSession(sessionConfig)
            callback(sessionResult)
        }
    }

    public suspend fun initializeSession(
        sessionConfig: MediaTailorSessionConfig
    ): SessionInitializationResult = withContext(Dispatchers.Main) {
        if (session != null) {
            return@withContext SessionInitializationResult.Failure("Session already initialized. Destroy the session before initializing a new one.")
        }
        val session = DefaultMediaTailorSession(player, httpClient, adMapper, sessionConfig)
        val sessionInitResult = session.initialize(sessionConfig)

        if (sessionInitResult is SessionInitializationResult.Success) {
            adPlaybackTracker = DefaultMediaTailorAdPlaybackTracker(player, session)
            adBeaconing = DefaultMediaTailorAdBeaconing(player, adPlaybackTracker!!, httpClient)
            this@MediaTailorSessionManager.session = session
        }

        sessionInitResult
    }

    public fun destroy() {
        adPlaybackTracker?.dispose()
        adPlaybackTracker = null
        adBeaconing?.dispose()
        adBeaconing = null
        session?.dispose()
        session = null
        scope.cancel()
    }
}
