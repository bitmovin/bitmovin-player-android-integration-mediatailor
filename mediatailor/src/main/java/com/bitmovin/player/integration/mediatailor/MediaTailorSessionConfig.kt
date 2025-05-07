package com.bitmovin.player.integration.mediatailor

data class MediaTailorSessionConfig(
    val sessionInitUrl: String,
    val assetType: MediaTailorAssetType,
    val sessionInitParams: Any? = null, // TODO
)

enum class MediaTailorAssetType {
    Vod,
    Linear,
}