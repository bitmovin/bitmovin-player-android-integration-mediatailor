package com.bitmovin.player.integration.mediatailor

import android.util.Log
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.source.SourceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MediaTailorPlayer(
    val player: Player,
) : Player by player {
    private val mediaTailorSession: MediaTailorSession = DefaultMediaTailorSession()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun load(sourceConfig: MediaTailorSessionConfig) {
        scope.launch {
            val result = mediaTailorSession.initialize(sourceConfig)
            val source = SourceConfig.fromUrl(result.getOrNull() ?: TODO("Failed to load session"))
            player.load(source)
        }
        player.on<PlayerEvent.TimeChanged> {
            Log.d("MediaTailorPlayer", "Time changed: ${it.time}")
        }
    }

    override fun destroy() {
        scope.cancel()
        player.destroy()
    }
}
