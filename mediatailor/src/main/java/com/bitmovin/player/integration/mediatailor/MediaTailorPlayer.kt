package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player

class MediaTailorPlayer(
    val player: Player,
) : Player by player {
}