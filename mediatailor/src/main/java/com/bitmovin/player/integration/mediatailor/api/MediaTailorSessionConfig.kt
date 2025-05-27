package com.bitmovin.player.integration.mediatailor.api

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for MediaTailor session initialization.
 */
public data class MediaTailorSessionConfig(
    /**
     * Url to the MediaTailor explicit session initialization endpoint.
     *
     * Reference: https://docs.aws.amazon.com/mediatailor/latest/ug/ad-reporting-client-side.html
     */
    public val sessionInitUrl: String,
    /**
     * Optional session initialization parameters.
     *
     * Reference: https://docs.aws.amazon.com/mediatailor/latest/ug/manifest-query-parameters-hls-and-dash-explicit-session-initialization.html
     */
    public val sessionInitParams: Map<String, Any> = emptyMap(),
    /**
     * The asset type of the MediaTailor session.
     */
    public val assetType: MediaTailorAssetType,
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
         * The configuration for the linear asset.
         */
        public val config: LinearAssetTypeConfig = LinearAssetTypeConfig(),
    ) : MediaTailorAssetType()
}

/**
 * Configuration for the linear asset types.
 */
public class LinearAssetTypeConfig(
    /**
     * The interval in seconds at which the TrackingUrl is polled.
     *
     * Reference: https://docs.aws.amazon.com/mediatailor/latest/ug/ad-reporting-client-side.html#ad-reporting-client-side-best-practices
     */
    public val trackingRequestPollFrequency: Duration = 4.seconds,
)
