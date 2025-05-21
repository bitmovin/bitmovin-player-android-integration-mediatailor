package com.bitmovin.player.integration.mediatailor.api

public sealed class MediaTailorEvent(
    /**
     * The time at which the event was emitted in milliseconds since the Unix Epoch.
     */
    public val timestamp: Long = System.currentTimeMillis()
) {
    public data class AdStarted(
        val ad: MediaTailorLinearAd,
    ) : MediaTailorEvent()

    public data class AdProgress(
        val ad: MediaTailorLinearAd,
        val progress: Double,
    ) : MediaTailorEvent()

    public data class AdFinished(
        val ad: MediaTailorLinearAd,
    ) : MediaTailorEvent()

    public data class AdBreakStarted(
        val adBreak: MediaTailorAdBreak,
    ) : MediaTailorEvent()

    public data class AdBreakFinished(
        val adBreak: MediaTailorAdBreak,
    ) : MediaTailorEvent()

    public data class UpcomingAdBreakUpdate(
        val adBreak: MediaTailorAdBreak?,
    ) : MediaTailorEvent()

    public data class Info(
        val message: String,
    ) : MediaTailorEvent()

    public data class Error(
        val message: String,
    ) : MediaTailorEvent()
}
