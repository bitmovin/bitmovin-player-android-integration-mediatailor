package com.bitmovin.player.integration.mediatailor.api

public class MediaTailorAdBreak(
    val id: String,
    val ads: List<MediaTailorLinearAd>,
    val scheduleTime: Double,
    val duration: Double,
)

public class MediaTailorLinearAd(
    val id: String,
    val scheduleTime: Double,
    val duration: Double,
    val trackingEvents: List<MediaTailorTrackingEvent>,
)

public class MediaTailorTrackingEvent(
    val id: String,
    val scheduleTime: Double,
    val duration: Double,
    val eventType: String,
    val beaconUrls: List<String>,
)
