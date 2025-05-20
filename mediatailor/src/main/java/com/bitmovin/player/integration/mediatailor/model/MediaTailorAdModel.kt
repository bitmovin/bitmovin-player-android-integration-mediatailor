package com.bitmovin.player.integration.mediatailor.model

import com.bitmovin.player.api.advertising.AdBreak
import com.bitmovin.player.api.advertising.AdData
import com.bitmovin.player.api.advertising.LinearAd
import com.bitmovin.player.api.advertising.LinearAdUiConfig

public class MediaTailorAdBreak(
    override val id: String,
    override val ads: List<MediaTailorLinearAd>,
    override val scheduleTime: Double,
    val duration: Double,
    override val replaceContentDuration: Double? = null
) : AdBreak

public class MediaTailorLinearAd(
    override val id: String?,
    val scheduleTime: Double,
    override val duration: Double,
    val trackingEvents: List<MediaTailorTrackingEvent> = emptyList(),
    override val uiConfig: LinearAdUiConfig? = LinearAdUiConfig(requestsUi = true),
    override val skippableAfter: Double? = null,
    override val clickThroughUrl: String? = null,
    override val data: AdData? = null,
    override val isLinear: Boolean = true,
    override val mediaFileUrl: String? = null,
    override val height: Int = 0,
    override val width: Int = 0,
) : LinearAd {
    override fun clickThroughUrlOpened() {
        TODO("Not yet implemented")
    }
}

public class MediaTailorTrackingEvent(
    val id: String,
    val startTime: Double,
    val duration: Double,
    val eventType: String,
    val beaconUrls: List<String>,
)