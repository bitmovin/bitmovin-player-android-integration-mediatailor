package com.bitmovin.player.integration.mediatailor

import android.util.Log
import com.bitmovin.player.api.Player
import com.bitmovin.player.core.internal.extensionPoint
import com.bitmovin.player.integration.mediatailor.network.DefaultHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
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
    private val scope = CoroutineScope(Dispatchers.Main)

    fun load(mediaTailorSourceConfig: MediaTailorSourceConfig) {
        scope.launch {
            val result = mediaTailorSession.initialize(mediaTailorSourceConfig)
            result.fold(
                onSuccess = {
                    Log.d("MediaTailorPlayer", "Session initialized successfully")
                    player.load(it)
                },
                onFailure = {
                    Log.e("MediaTailorPlayer", "Failed to initialize session: ${it.message}")
                }
            )
        }
    }

    fun getCurrentAdBreak(): MediaTailorAdBreak? {
        return mediaTailorSession.adBreaks.find {
            currentTime in (it.scheduleTime..it.scheduleTime + it.duration)
        }
    }

    override fun destroy() {
        mediaTailorSession.dispose()
        scope.cancel()
        player.destroy()
    }
}
