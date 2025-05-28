package com.bitmovin.player.integration.mediatailor.model

import kotlinx.serialization.Serializable

@Serializable
internal data class MediaTailorSessionInitializationResponse(
    val manifestUrl: String,
    val trackingUrl: String
)
