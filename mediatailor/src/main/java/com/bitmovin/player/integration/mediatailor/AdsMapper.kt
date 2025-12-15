package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.api.MediaTailorLinearAd
import com.bitmovin.player.integration.mediatailor.api.MediaTailorTrackingEvent
import com.bitmovin.player.integration.mediatailor.model.Avail

internal interface AdsMapper {
    fun mapAdBreaks(avails: List<Avail>): List<MediaTailorAdBreak>
}

internal class DefaultAdsMapper : AdsMapper {
    override fun mapAdBreaks(avails: List<Avail>): List<MediaTailorAdBreak> {
        return avails.map { avail ->
            MediaTailorAdBreak(
                id = avail.availId,
                ads = avail.ads.map { ad ->
                    MediaTailorLinearAd(
                        id = ad.adId,
                        scheduleTime = ad.startTimeInSeconds,
                        duration = ad.durationInSeconds,
                        formattedDuration = ad.duration,
                        trackingEvents = ad.trackingEvents.map { trackingEvent ->
                            MediaTailorTrackingEvent(
                                id = trackingEvent.eventId,
                                scheduleTime = trackingEvent.startTimeInSeconds,
                                duration = trackingEvent.durationInSeconds,
                                eventType = trackingEvent.eventType,
                                beaconUrls = trackingEvent.beaconUrls,
                            )
                        },
                    )
                },
                scheduleTime = avail.startTimeInSeconds,
                duration = avail.durationInSeconds,
                formattedDuration = avail.duration,
                adMarkerDuration = avail.adMarkerDuration,
            )
        }
    }
}
