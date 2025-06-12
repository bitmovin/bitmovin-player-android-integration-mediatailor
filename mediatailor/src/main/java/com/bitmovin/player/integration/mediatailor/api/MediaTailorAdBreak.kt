package com.bitmovin.player.integration.mediatailor.api

public class MediaTailorAdBreak(
    public val id: String,
    public val ads: List<MediaTailorLinearAd>,
    /**
     * Schedule time of the ad break in seconds.
     */
    public val scheduleTime: Double,
    /**
     * Duration of the ad break in seconds.
     */
    public val duration: Double,
)

public class MediaTailorLinearAd(
    public val id: String,
    /**
     * Schedule time of the ad in seconds.
     */
    public val scheduleTime: Double,
    /**
     * Duration of the ad in seconds.
     */
    public val duration: Double,
    public val trackingEvents: List<MediaTailorTrackingEvent>,
) {
    /**
     * Ad click through url, if present.
     *
     * To track the click through event,
     * use the [MediaTailorSessionManager.sendTrackingEvent] with [TrackingEvent.ClickTracking].
     */
    public val clickThroughUrl: String?
        get() = trackingEvents
            .find { it.eventType == "clickThrough" }
            ?.beaconUrls
            ?.find { it.isNotBlank() }
}

public class MediaTailorTrackingEvent(
    public val id: String,
    /**
     * Schedule time of the tracking event in seconds.
     */
    public val scheduleTime: Double,
    /**
     * Duration of the tracking event in seconds.
     */
    public val duration: Double,
    /**
     * Type of the tracking event.
     *
     * See [TrackingEvent] for the list of supported event types.
     */
    public val eventType: String,
    /**
     * [MediaTailorSessionManager] will send a GET request to each of these URLs when tracking the event.
     */
    public val beaconUrls: List<String>,
)
