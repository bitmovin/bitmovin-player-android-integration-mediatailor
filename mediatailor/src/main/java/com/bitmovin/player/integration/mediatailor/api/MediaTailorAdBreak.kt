package com.bitmovin.player.integration.mediatailor.api

public class MediaTailorAdBreak(
    public val id: String,
    public val ads: List<MediaTailorLinearAd>,
    public val scheduleTime: Double,
    public val duration: Double,
)

public class MediaTailorLinearAd(
    public val id: String,
    public val scheduleTime: Double,
    public val duration: Double,
    public val trackingEvents: List<MediaTailorTrackingEvent>,
)

public class MediaTailorTrackingEvent(
    public val id: String,
    public val scheduleTime: Double,
    public val duration: Double,
    public val eventType: String,
    public val beaconUrls: List<String>,
)
