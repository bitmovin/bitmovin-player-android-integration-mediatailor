package com.bitmovin.player.integration.mediatailor.api

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.integration.mediatailor.DefaultMediaTailorAdPlaybackTracker
import com.bitmovin.player.integration.mediatailor.DefaultMediaTailorAdsMapper
import com.bitmovin.player.integration.mediatailor.DefaultMediaTailorSession
import com.bitmovin.player.integration.mediatailor.MediaTailorAdPlaybackTracker
import com.bitmovin.player.integration.mediatailor.MediaTailorSession
import com.bitmovin.player.integration.mediatailor.DefaultMediaTailorAdBeaconing
import com.bitmovin.player.integration.mediatailor.MediaTailorAdBeaconing
import com.bitmovin.player.integration.mediatailor.network.DefaultHttpClient
import com.bitmovin.player.integration.mediatailor.util.Disposable
import com.bitmovin.player.integration.mediatailor.util.eventFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

public class MediaTailorSessionManager(
    val player: Player,
) : Disposable {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val httpClient = DefaultHttpClient()
    private val adMapper = DefaultMediaTailorAdsMapper()
    private var session: MediaTailorSession? = null
    private var adPlaybackTracker: MediaTailorAdPlaybackTracker? = null
    private var adBeaconing: MediaTailorAdBeaconing? = null

    init {
        scope.launch {
            player.eventFlow<PlayerEvent.Destroy>().collect {
                dispose()
            }
        }
    }

    public fun initializeSession(
        sessionConfig: MediaTailorSessionConfig,
        callback: (Result<String>) -> Unit
    ) {
        scope.launch {
            val sessionResult = initializeSession(sessionConfig)
            callback(sessionResult)
        }
    }

    public suspend fun initializeSession(
        sessionConfig: MediaTailorSessionConfig
    ): Result<String> {
        val session = DefaultMediaTailorSession(player, httpClient, adMapper)
        val sessionInitResult = session.initialize(sessionConfig)

        if (sessionInitResult.isSuccess) {
            adPlaybackTracker = DefaultMediaTailorAdPlaybackTracker(player, session)
            adBeaconing = DefaultMediaTailorAdBeaconing(player, adPlaybackTracker!!, httpClient)
            this@MediaTailorSessionManager.session = session
        }

        return sessionInitResult
    }

    override fun dispose() {
        adPlaybackTracker?.dispose()
        adPlaybackTracker = null
        adBeaconing?.dispose()
        adBeaconing = null
        session?.dispose()
        session = null
        scope.cancel()
    }
}