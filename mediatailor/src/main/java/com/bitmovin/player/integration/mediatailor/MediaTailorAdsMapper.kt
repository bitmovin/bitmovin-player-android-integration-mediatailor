package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.integration.mediatailor.model.Avail

internal interface MediaTailorAdsMapper {
    fun mapAdBreaks(avails: List<Avail>): List<MediaTailorAdBreak>
}

internal class DefaultMediaTailorAdsMapper : MediaTailorAdsMapper {
    override fun mapAdBreaks(avails: List<Avail>): List<MediaTailorAdBreak> {
        return avails.map { avail ->
            MediaTailorAdBreak(
                id = avail.availId,
                ads = avail.ads.map { ad ->
                    MediaTailorLinearAd(
                        id = ad.adId,
                        duration = ad.durationInSeconds,
                    )
                },
                scheduleTime = avail.startTimeInSeconds,
                duration = avail.durationInSeconds,
            )
        }
    }
}
