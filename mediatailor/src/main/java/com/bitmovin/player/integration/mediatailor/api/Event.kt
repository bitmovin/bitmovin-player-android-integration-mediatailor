package com.bitmovin.player.integration.mediatailor.api

/**
 * Includes all events that can be emitted by the [MediaTailorSessionManager].
 */
public sealed class MediaTailorEvent(
    /**
     * The time at which the event was emitted in milliseconds since the Unix Epoch.
     */
    public val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Emitted when the playback of an ad has started.
     */
    public class AdStarted(
        /**
         * The ad that has started playback.
         */
        public val ad: MediaTailorLinearAd,
        /**
         * The index of the ad in the queue.
         */
        public val indexInQueue: Int,
    ) : MediaTailorEvent()

    /**
     * Emitted when an ad has finished playback.
     */
    public class AdFinished(
        /**
         * The ad that has finished playback.
         */
        public val ad: MediaTailorLinearAd,
    ) : MediaTailorEvent()

    /**
     * Emitted when an ad break has started.
     */
    public class AdBreakStarted(
        /**
         * The ad break that has started.
         */
        public val adBreak: MediaTailorAdBreak,
    ) : MediaTailorEvent()

    /**
     * Emitted when an ad break has finished.
     */
    public class AdBreakFinished(
        /**
         * The ad break that has finished.
         */
        public val adBreak: MediaTailorAdBreak,
    ) : MediaTailorEvent()

    /**
     * Emitted when an upcoming ad break is detected.
     */
    public class UpcomingAdBreakUpdated(
        /**
         * The ad break that is upcoming or `null` if currently no upcoming ad break is known.
         */
        public val adBreak: MediaTailorAdBreak?,
    ) : MediaTailorEvent()

    /**
     * Emitted when the ad breaks schedule has been updated.
     */
    public class AdBreakScheduleUpdated(
        /**
         * The list of ad breaks that are currently scheduled.
         */
        public val adBreaks: List<MediaTailorAdBreak>,
    ) : MediaTailorEvent()

    /**
     * Emitted for neutral information provided by [MediaTailorSessionManager].
     */
    public class Info(
        /**
         * A message describing the information.
         */
        public val message: String,
    ) : MediaTailorEvent()

    /**
     * Emitted for error information provided by [MediaTailorSessionManager].
     */
    public class Error(
        /**
         * A message describing the error.
         */
        public val message: String,
    ) : MediaTailorEvent()
}
