package com.bitmovin.player.integration.mediatailor

sealed class MediaTailorSessionConfig(
    open val assetType: MediaTailorAssetType,
) {
    data class Implicit(
        override val assetType: MediaTailorAssetType,
        val sessionInitUrl: String,
    ) : MediaTailorSessionConfig(
        assetType
    )

    data class Explicit(
        val manifestUrl: String,
        val trackingUrl: String,
        override val assetType: MediaTailorAssetType,
    ) : MediaTailorSessionConfig(
        assetType
    )
}

enum class MediaTailorAssetType {
    Vod,
    Linear,
}