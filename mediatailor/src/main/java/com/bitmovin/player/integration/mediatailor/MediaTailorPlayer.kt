package com.bitmovin.player.integration.mediatailor

import android.util.Log
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.integration.mediatailor.model.MediaTailorTrackingSession
import com.bitmovin.player.integration.mediatailor.network.DefaultDataSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URL

class MediaTailorPlayer(
    val player: Player,
) : Player by player {
    private val dataSourceFactory = DefaultDataSourceFactory()
    private val sessionInitInitializer: MediaTailorSessionInitInitializer =
        DefaultMediaTailorSessionInitInitializer(dataSourceFactory)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    // TODO: Extract into a session
    private var sessionTrackingUrl: String? = null
    private var sessionTrackingResponse: MediaTailorTrackingSession? = null

    init {
        ioScope.launch {
            while (isActive) {
                fetchTrackingSession()
                // Web integration makes a request for every segment playback event. Android doesn't have that.
                delay(4_000)
            }
        }
    }

    fun load(sourceConfig: MediaTailorSessionConfig) {
        ioScope.launch {
            val source = initSession(sourceConfig)
            player.load(source)
        }
    }

    private suspend fun initSession(sourceConfig: MediaTailorSessionConfig): SourceConfig {
        when (sourceConfig) {
            is MediaTailorSessionConfig.Implicit -> {
                val response = sessionInitInitializer.prepareImplicitSession(sourceConfig)
                val manifestUrlBase = URL(sourceConfig.sessionInitUrl).let {
                    it.protocol + "://" + it.authority
                }
                val manifestUrl = manifestUrlBase + response.manifestUrl
                sessionTrackingUrl = manifestUrlBase + response.trackingUrl
                sessionTrackingResponse = sessionInitInitializer.initialize(
                    MediaTailorSessionConfig.Explicit(
                        manifestUrl = manifestUrl,
                        trackingUrl = sessionTrackingUrl!!,
                    )
                )
                return SourceConfig.fromUrl(manifestUrl)
            }

            is MediaTailorSessionConfig.Explicit -> {
                sessionTrackingResponse = sessionInitInitializer.initialize(sourceConfig)
                sessionTrackingUrl = sourceConfig.trackingUrl
                return SourceConfig.fromUrl(sourceConfig.manifestUrl)
            }
        }
    }

    private suspend fun fetchTrackingSession() {
        val sessionTrackingUrl = sessionTrackingUrl ?: return
        val response = sessionInitInitializer.refresh(sessionTrackingUrl)
        Log.d("MediaTailorPlayer", "Tracking session: $response")
    }

    override fun destroy() {
        ioScope.cancel()
        player.destroy()
    }
}
