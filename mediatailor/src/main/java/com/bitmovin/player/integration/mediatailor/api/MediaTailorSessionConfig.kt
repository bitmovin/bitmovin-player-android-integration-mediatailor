package com.bitmovin.player.integration.mediatailor.api

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

public sealed class MediaTailorAssetType {
    /**
     * https://docs.aws.amazon.com/mediatailor/latest/ug/channel-assembly-working-vod-sources.html
     */
    public object Vod : MediaTailorAssetType()

    /**
     * https://docs.aws.amazon.com/mediatailor/latest/ug/channel-assembly-working-live-sources.html
     */
    public class Linear(
        /**
         * The interval in seconds at which the TrackingUrl is polled.
         *
         * Reference: https://docs.aws.amazon.com/mediatailor/latest/ug/ad-reporting-client-side.html#ad-reporting-client-side-best-practices
         */
        val trackingRequestPollFrequency: Double = 4.0
    ) : MediaTailorAssetType()
}
