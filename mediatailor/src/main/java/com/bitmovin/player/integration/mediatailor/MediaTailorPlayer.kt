package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.integration.mediatailor.network.DefaultDataSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MediaTailorPlayer(
    val player: Player,
) : Player by player {
    private val dataSourceFactory = DefaultDataSourceFactory()
    private val mediaTailorSession: MediaTailorSession = DefaultMediaTailorSession(
        dataSourceFactory = dataSourceFactory,
    )
    private val scope = CoroutineScope(Dispatchers.Main)

    fun load(sourceConfig: MediaTailorSessionConfig) {
        scope.launch {
            val result = mediaTailorSession.initialize(sourceConfig)
            val source = SourceConfig.fromUrl(result.getOrNull() ?: TODO("Failed to load session"))
            player.load(source)
        }
    }

    override fun destroy() {
        scope.cancel()
        player.destroy()
    }
}
