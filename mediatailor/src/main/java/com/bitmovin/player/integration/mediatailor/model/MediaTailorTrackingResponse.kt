package com.bitmovin.player.integration.mediatailor.model

import kotlinx.serialization.Serializable

@Serializable
internal data class MediaTailorTrackingResponse(
    val avails: List<Avail>,
)

@Serializable
internal data class Avail(
    val ads: List<Ad>,
    val availId: String,
    val duration: String,
    val durationInSeconds: Double,
    val startTime: String,
    val startTimeInSeconds: Double,
    val adMarkerDuration: String,
)

@Serializable
internal data class Ad(
    val adId: String,
    val duration: String,
    val durationInSeconds: Double,
    val startTime: String,
    val startTimeInSeconds: Double,
    val trackingEvents: List<TrackingEvent>,
    val companionAds: List<CompanionAd>,
)

@Serializable
internal data class TrackingEvent(
    val beaconUrls: List<String>,
    val duration: String,
    val durationInSeconds: Double,
    val eventId: String,
    val eventType: String,
    val startTime: String,
    val startTimeInSeconds: Double,
)

@Serializable
internal data class CompanionAd(
    val attributes: CompanionAttributes,
    val companionClickThrough: String?,
    val companionClickTracking: String?,
    val htmlResource: String?,
    val iFrameResource: String?,
    val staticResource: String?,
    val trackingEvents: List<CompanionTrackingEvent>,
)

@Serializable
internal data class CompanionTrackingEvent(
    val event: String,
    val uri: String,
)

@Serializable
internal data class CompanionAttributes(
    val id: String,
    val apiFramework: String?,
    val height: String?,
    val width: String?,
)
