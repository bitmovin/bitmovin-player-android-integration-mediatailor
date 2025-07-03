package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.model.Ad
import com.bitmovin.player.integration.mediatailor.model.Avail
import com.bitmovin.player.integration.mediatailor.model.TrackingEvent
import io.kotest.core.spec.style.DescribeSpec
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

class AdsMapperSpec : DescribeSpec({
    val adsMapper = DefaultAdsMapper()
    val avails = listOf(
        Avail(
            availId = "avail1",
            startTimeInSeconds = 10.0,
            durationInSeconds = 30.0,
            ads = listOf(
                Ad(
                    adId = "ad1",
                    startTimeInSeconds = 10.0,
                    durationInSeconds = 20.0,
                    startTime = "startTime1",
                    duration = "duration1",
                    trackingEvents = listOf(
                        TrackingEvent(
                            eventId = "event1",
                            startTimeInSeconds = 10.0,
                            durationInSeconds = 5.0,
                            eventType = "start",
                            beaconUrls = listOf("http://example.com/beacon1"),
                            startTime = "startTimeEvent1",
                            duration = "durationEvent1",
                        ),
                    ),
                ),
                Ad(
                    adId = "ad2",
                    startTimeInSeconds = 40.0,
                    durationInSeconds = 15.0,
                    startTime = "startTime2",
                    duration = "duration2",
                    trackingEvents = listOf(
                        TrackingEvent(
                            eventId = "event2",
                            startTimeInSeconds = 40.0,
                            durationInSeconds = 5.0,
                            eventType = "complete",
                            beaconUrls = listOf("http://example.com/beacon2"),
                            startTime = "startTimeEvent2",
                            duration = "durationEvent2",
                        ),
                    ),
                ),
            ),
            duration = "duration1",
            startTime = "startTime1",
            adMarkerDuration = "adMarkerDuration1",
        ),
        Avail(
            availId = "avail2",
            startTimeInSeconds = 50.0,
            durationInSeconds = 30.0,
            ads = emptyList(),
            duration = "duration2",
            startTime = "startTime2",
            adMarkerDuration = "adMarkerDuration2",
        ),
    )

    it("maps avails to media tailor ad break") {
        val adBreaks: List<MediaTailorAdBreak> = adsMapper.mapAdBreaks(avails)

        expectThat(adBreaks)
            .hasSize(2)
            .and {
                get { first() }.and {
                    get { id } isEqualTo "avail1"
                    get { scheduleTime } isEqualTo 10.0
                    get { duration } isEqualTo 30.0
                    get { ads }.hasSize(2)
                    get { formattedDuration } isEqualTo "duration1"
                    get { adMarkerDuration } isEqualTo "adMarkerDuration1"
                }
            }
            .and {
                get { get(1) }.and {
                    get { id } isEqualTo "avail2"
                    get { scheduleTime } isEqualTo 50.0
                    get { duration } isEqualTo 30.0
                    get { ads }.hasSize(0)
                    get { formattedDuration } isEqualTo "duration2"
                    get { adMarkerDuration } isEqualTo "adMarkerDuration2"
                }
            }
    }

    it("maps ads to media tailor linear ad") {
        val adBreaks: List<MediaTailorAdBreak> = adsMapper.mapAdBreaks(avails)

        expectThat(adBreaks.first().ads)
            .hasSize(2)
            .and {
                get { first() }.and {
                    get { id } isEqualTo "ad1"
                    get { scheduleTime } isEqualTo 10.0
                    get { duration } isEqualTo 20.0
                    get { trackingEvents }.hasSize(1)
                }
            }
            .and {
                get { get(1) }.and {
                    get { id } isEqualTo "ad2"
                    get { scheduleTime } isEqualTo 40.0
                    get { duration } isEqualTo 15.0
                    get { trackingEvents }.hasSize(1)
                }
            }
    }

    it("maps tracking events to media tailor tracking event") {
        val adBreaks: List<MediaTailorAdBreak> = adsMapper.mapAdBreaks(avails)

        expectThat(adBreaks.first().ads.first().trackingEvents)
            .hasSize(1)
            .and {
                get { first() }.and {
                    get { id } isEqualTo "event1"
                    get { scheduleTime } isEqualTo 10.0
                    get { duration } isEqualTo 5.0
                    get { eventType } isEqualTo "start"
                    get { beaconUrls }.hasSize(1).first() isEqualTo "http://example.com/beacon1"
                }
            }
    }
})
