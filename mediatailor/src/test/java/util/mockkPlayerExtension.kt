package util

import io.mockk.mockkStatic
import io.mockk.unmockkStatic

private val playerExtensionAbsolutePath =
    "com.bitmovin.player.integration.mediatailor.util.PlayerExtension"

internal fun mockkPlayerExtension() = mockkStatic(playerExtensionAbsolutePath)
internal fun unmockkPlayerExtension() = unmockkStatic(playerExtensionAbsolutePath)