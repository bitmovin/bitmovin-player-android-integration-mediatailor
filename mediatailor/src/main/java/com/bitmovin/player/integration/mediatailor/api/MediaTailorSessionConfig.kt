package com.bitmovin.player.integration.mediatailor.api

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for session initialization with [MediaTailorSessionManager].
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
    /**
     * Whether automatic ad tracking events are enabled for the session.
     * - When set to `true`, the player will automatically send tracking events,
     *   Check out [PlayerTrackingEvents] and [LinearAdTrackingEvents]
     *   to see which events are tracked by default.
     * - When set to `false`, no ad tracking events will be sent automatically.
     */
    public val automaticAdTrackingEnabled: Boolean = true,
)

public sealed class MediaTailorAssetType {
    /**
     * Vod asset type.
     *
     * https://docs.aws.amazon.com/mediatailor/latest/ug/channel-assembly-working-vod-sources.html
     */
    public object Vod : MediaTailorAssetType()

    /**
     * Linear asset type.
     *
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
