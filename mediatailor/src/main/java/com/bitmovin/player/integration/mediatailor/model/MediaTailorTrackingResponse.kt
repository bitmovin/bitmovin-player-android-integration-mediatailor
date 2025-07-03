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
