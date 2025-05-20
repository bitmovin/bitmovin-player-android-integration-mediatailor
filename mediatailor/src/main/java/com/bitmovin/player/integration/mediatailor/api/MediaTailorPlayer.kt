package com.bitmovin.player.integration.mediatailor.api

import android.util.Log
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.source.SourceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MediaTailorPlayer(
    private val player: Player,
) : Player by player {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val mediaTailorSessionManager = MediaTailorSessionManager(player)

    fun load(mediaTailorSourceConfig: MediaTailorSourceConfig) {
        scope.launch {
            val sessionResult =
                mediaTailorSessionManager.initializeSession(mediaTailorSourceConfig.sessionConfig)

            sessionResult.fold(
                onSuccess = {
                    Log.d("MediaTailorPlayer", "Session initialized successfully")
                    player.load(SourceConfig.Companion.fromUrl(it))
                },
                onFailure = {
                    Log.e("MediaTailorPlayer", "Failed to initialize session: ${it.message}")
                }
            )
        }
    }


    override fun destroy() {
        mediaTailorSessionManager.dispose()
        scope.cancel()
        player.destroy()
    }
}