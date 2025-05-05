package com.bitmovin.player.integration.mediatailor.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaTailorTrackingResponse(
    val avails: List<Avail>
)

@Serializable
data class Avail(
    val ads: List<Ad>,
    val availId: String,
    val duration: String,
    val durationInSeconds: Double,
    val startTime: String,
    val startTimeInSeconds: Double
)

@Serializable
data class Ad(
    val adId: String,
    val duration: String,
    val durationInSeconds: Double,
    val startTime: String,
    val startTimeInSeconds: Double,
    val trackingEvents: List<TrackingEvent>
)

@Serializable
data class TrackingEvent(
    val beaconUrls: List<String>,
    val duration: String,
    val durationInSeconds: Double,
    val eventId: String,
    val eventType: String,
    val startTime: String,
    val startTimeInSeconds: Double
)

@Serializable
data class ImplicitSessionStartResponse(
    val manifestUrl: String,
    val trackingUrl: String
)
