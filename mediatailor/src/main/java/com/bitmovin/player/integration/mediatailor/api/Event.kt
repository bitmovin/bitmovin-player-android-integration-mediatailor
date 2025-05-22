package com.bitmovin.player.integration.mediatailor.api

public sealed class MediaTailorEvent(
    /**
     * The time at which the event was emitted in milliseconds since the Unix Epoch.
     */
    public val timestamp: Long = System.currentTimeMillis()
) {
    public class AdStarted(
        public val ad: MediaTailorLinearAd,
    ) : MediaTailorEvent()

    public class AdProgress(
        public val ad: MediaTailorLinearAd,
        public val progress: Double,
    ) : MediaTailorEvent()

    public class AdFinished(
        public val ad: MediaTailorLinearAd,
    ) : MediaTailorEvent()

    public class AdBreakStarted(
        public val adBreak: MediaTailorAdBreak,
    ) : MediaTailorEvent()

    public class AdBreakFinished(
        public val adBreak: MediaTailorAdBreak,
    ) : MediaTailorEvent()

    public class UpcomingAdBreakUpdate(
        public val adBreak: MediaTailorAdBreak?,
    ) : MediaTailorEvent()

    public class Info(
        public val message: String,
    ) : MediaTailorEvent()

    public class Error(
        public val message: String,
    ) : MediaTailorEvent()
}
