package com.bitmovin.player.integration.mediatailor.api

/**
 * Represents a MediaTailor Avail.
 *
 * Reference:
 * https://docs.aws.amazon.com/mediatailor/latest/ug/ad-reporting-client-side-ad-tracking-schema.html
 */
public data class MediaTailorAdBreak(
    /**
     * Represents MediaTailor `availId`.
     */
    public val id: String,
    public val ads: List<MediaTailorLinearAd>,
    /**
     * Schedule time of the ad break in seconds.
     *
     * Represents MediaTailor `startTimeInSeconds`.
     */
    public val scheduleTime: Double,
    /**
     * Duration of the ad break in seconds.
     *
     * Represents MediaTailor `durationInSeconds`.
     */
    public val duration: Double,
    /**
     * Duration, in ISO 8601 seconds format.
     *
     * Represents MediaTailor `duration`.
     */
    public val formattedDuration: String,
    /**
     * The duration observed from the ad marker in the manifest.
     *
     * Represents MediaTailor `adMarkerDuration`.
     */
    public val adMarkerDuration: String,
)

/**
 * Represents a MediaTailor Linear Ad.
 */
public data class MediaTailorLinearAd(
    /**
     * Represents MediaTailor `adId`.
     */
    public val id: String,
    /**
     * Schedule time of the ad in seconds.
     *
     * Represents MediaTailor `startTimeInSeconds`.
     */
    public val scheduleTime: Double,
    /**
     * Duration of the ad in seconds.
     *
     * Represents MediaTailor `durationInSeconds`.
     */
    public val duration: Double,
    /**
     * Duration, in ISO 8601 seconds format.
     *
     * Represents MediaTailor `duration`.
     */
    public val formattedDuration: String,
    public val trackingEvents: List<MediaTailorTrackingEvent>,
    public val companionAds: List<MediaTailorCompanionAd>
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

public data class MediaTailorTrackingEvent(
    /**
     * Represents MediaTailor `eventId`.
     */
    public val id: String,
    /**
     * Schedule time of the tracking event in seconds.
     *
     * Represents MediaTailor `startTimeInSeconds`.
     */
    public val scheduleTime: Double,
    /**
     * Duration of the tracking event in seconds.
     *
     * Represents MediaTailor `durationInSeconds`.
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

public data class MediaTailorCompanionAd(
    val attributes: MediaTailorCompanionAttributes,
    val companionClickThrough: String?,
    val companionClickTracking: String?,
    val htmlResource: String?,
    val iFrameResource: String?,
    val staticResource: String?,
    val trackingEvents: List<MediaTailorCompanionTrackingEvent>,
) {
    public fun getId(): String {
        return attributes.id
    }

    public fun getResourceByType(type: MediaTailorCompanionResourceType): String? {
        return when (type) {
            MediaTailorCompanionResourceType.HTML -> htmlResource
            MediaTailorCompanionResourceType.STATIC -> staticResource
            MediaTailorCompanionResourceType.IFRAME -> iFrameResource
        }
    }

    public fun getAvailableResourceTypes(): List<MediaTailorCompanionResourceType> {
        val types: MutableList<MediaTailorCompanionResourceType> = mutableListOf()
        htmlResource?.let {
            types.add(MediaTailorCompanionResourceType.HTML)
        }
        staticResource?.let {
            types.add(MediaTailorCompanionResourceType.STATIC)
        }
        iFrameResource?.let {
            types.add(MediaTailorCompanionResourceType.IFRAME)
        }
        return types
    }
}

public data class MediaTailorCompanionTrackingEvent(
    val event: String,
    val uri: String,
)

public data class MediaTailorCompanionAttributes(
    val id: String,
    val apiFramework: String,
    val height: Int,
    val width: Int,
)

public enum class MediaTailorCompanionResourceType {
    HTML,
    STATIC,
    IFRAME,
}
