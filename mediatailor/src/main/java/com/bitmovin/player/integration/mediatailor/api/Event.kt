package com.bitmovin.player.integration.mediatailor.api

public sealed class MediaTailorEvent(
    /**
     * The time at which the event was emitted in milliseconds since the Unix Epoch.
     */
    public val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * An ad has started playing.
     *
     * Use [ad] to access the details of the ad.
     * Use [indexInQueue] to access the index of the ad within the ad break.
     */
    public class AdStarted(
        public val ad: MediaTailorLinearAd,
        /**
         * The index of the ad in the queue.
         */
        public val indexInQueue: Int,
    ) : MediaTailorEvent()

    /**
     * Emitted when an ad has finished playing.
     *
     * Use [ad] to access the details of the ad.
     */
    public class AdFinished(
        public val ad: MediaTailorLinearAd,
    ) : MediaTailorEvent()

    /**
     * Emitted when an ad break has started.
     *
     * Use [adBreak] to access the details of the ad break.
     */
    public class AdBreakStarted(
        public val adBreak: MediaTailorAdBreak,
    ) : MediaTailorEvent()

    /**
     * Emitted when an ad break has finished.
     *
     * Use [adBreak] to access the details of the ad break.
     */
    public class AdBreakFinished(
        public val adBreak: MediaTailorAdBreak,
    ) : MediaTailorEvent()

    /**
     * Emitted when an upcoming ad break is detected.
     *
     * Use [adBreak] to access the details of the ad break.
     */
    public class UpcomingAdBreakUpdate(
        public val adBreak: MediaTailorAdBreak?,
    ) : MediaTailorEvent()

    /**
     * Emitted for neutral information provided by the media tailor session.
     *
     * Check [message] for details.
     */
    public class Info(
        public val message: String,
    ) : MediaTailorEvent()

    /**
     * An error has occurred during the MediaTailor session.
     * Check [message] for details.
     */
    public class Error(
        public val message: String,
    ) : MediaTailorEvent()
}
