package com.bitmovin.player.integration.mediatailor.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaTailorSessionInitializationResponse(
    val manifestUrl: String,
    val trackingUrl: String
)
