package util

import io.mockk.mockkStatic
import io.mockk.unmockkStatic

private const val PLAYER_EXTENSION_ABSOLUTE_PATH =
    "com.bitmovin.player.integration.mediatailor.util.PlayerExtension"

internal fun mockkPlayerExtension() = mockkStatic(PLAYER_EXTENSION_ABSOLUTE_PATH)
internal fun unmockkPlayerExtension() = unmockkStatic(PLAYER_EXTENSION_ABSOLUTE_PATH)
