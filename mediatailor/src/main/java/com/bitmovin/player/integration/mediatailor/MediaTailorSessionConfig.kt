package com.bitmovin.player.integration.mediatailor

public data class MediaTailorSessionConfig(
    /**
     * Url to the MediaTailor explicit session initialization endpoint.
     *
     * Reference: https://docs.aws.amazon.com/mediatailor/latest/ug/ad-reporting-client-side.html
     */
    val sessionInitUrl: String,
    /**
     * Optional session initialization parameters.
     *
     * Reference: https://docs.aws.amazon.com/mediatailor/latest/ug/manifest-query-parameters-hls-and-dash-explicit-session-initialization.html
     */
    val sessionInitParams: Map<String, Any> = emptyMap(),
    /**
     * The asset type of the MediaTailor session.
     */
    val assetType: MediaTailorAssetType,
)

public enum class MediaTailorAssetType {
    Vod,
    Linear,
}
