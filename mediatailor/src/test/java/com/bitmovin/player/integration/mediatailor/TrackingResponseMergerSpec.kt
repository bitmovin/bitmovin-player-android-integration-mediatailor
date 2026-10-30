package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.integration.mediatailor.model.Ad
import com.bitmovin.player.integration.mediatailor.model.Avail
import com.bitmovin.player.integration.mediatailor.model.TrackingEvent
import io.kotest.core.spec.style.DescribeSpec
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

class TrackingResponseMergerSpec : DescribeSpec({
    describe("mergeAvails") {
        it("adds avails that are not known yet") {
            val result = mergeAvails(listOf(avail("1", 10.0)), listOf(avail("2", 50.0)))

            expectThat(result.map { it.availId }).containsExactly("1", "2")
        }

        it("keeps known avails that are missing in the incoming avails") {
            val result = mergeAvails(listOf(avail("1", 10.0), avail("2", 50.0)), emptyList())

            expectThat(result.map { it.availId }).containsExactly("1", "2")
        }

        it("sorts the result by start time") {
            val result = mergeAvails(listOf(avail("2", 50.0)), listOf(avail("1", 10.0), avail("3", 90.0)))

            expectThat(result.map { it.availId }).containsExactly("1", "2", "3")
        }

        describe("when a known avail re-appears with only the new ad") {
            val existing = avail("1", 10.0, ads = listOf(ad("a1", 10.0, events("a1", 3)), ad("a2", 25.0, events("a2", 2))))
            val incoming = avail("1", 10.0, ads = listOf(ad("a3", 40.0, events("a3", 2))))

            it("keeps the known ads and adds the new one") {
                val result = mergeAvails(listOf(existing), listOf(incoming))

                expectThat(result).hasSize(1)
                expectThat(result.single().ads.map { it.adId }).containsExactly("a1", "a2", "a3")
            }
        }

        describe("when the timings of the same ids jitter") {
            val existing = avail(
                "1",
                10.0,
                duration = 90.08,
                ads = listOf(ad("a1", 6130.02, events("a1", 2))),
            )
            val incoming = avail(
                "1",
                10.0,
                duration = 90.156,
                ads = listOf(ad("a1", 6130.077, events("a1", 2))),
            )
            val result = mergeAvails(listOf(existing), listOf(incoming))

            it("does not duplicate avails or ads") {
                expectThat(result).hasSize(1)
                expectThat(result.single().ads).hasSize(1)
            }

            it("takes the latest timings") {
                expectThat(result.single().durationInSeconds).isEqualTo(90.156)
                expectThat(result.single().ads.single().startTimeInSeconds).isEqualTo(6130.077)
            }
        }

        describe("when an ad re-appears with new tracking events") {
            val existing = avail(
                "1",
                10.0,
                ads = listOf(
                    ad(
                        "a1",
                        10.0,
                        listOf(
                            event("a1", "impression", "http://x/impression-1"),
                            event("a1", "impression", "http://x/impression-2"),
                            event("a1", "start", "http://x/start"),
                        ),
                    ),
                ),
            )
            val incoming = avail(
                "1",
                10.0,
                ads = listOf(
                    ad(
                        "a1",
                        10.0,
                        listOf(
                            event("a1", "start", "http://x/start", startTime = 11.0),
                            event("a1", "firstQuartile", "http://x/q1"),
                            event("a1", "firstQuartile", "http://x/q1-other"),
                        ),
                    ),
                ),
            )
            val result = mergeAvails(listOf(existing), listOf(incoming)).single().ads.single()

            it("keeps events that share an eventId but have different beacon urls") {
                expectThat(result.trackingEvents.map { it.eventType to it.beaconUrls.single() })
                    .containsExactlyInAnyOrder(
                        "impression" to "http://x/impression-1",
                        "impression" to "http://x/impression-2",
                        "start" to "http://x/start",
                        "firstQuartile" to "http://x/q1",
                        "firstQuartile" to "http://x/q1-other",
                    )
            }

            it("takes the incoming event when the key collides") {
                expectThat(result.trackingEvents.single { it.eventType == "start" }.startTimeInSeconds)
                    .isEqualTo(11.0)
            }
        }

        describe("when an ad re-appears without tracking events") {
            it("keeps the known tracking events") {
                val existing = avail("1", 10.0, ads = listOf(ad("a1", 10.0, events("a1", 3))))
                val incoming = avail("1", 10.0, ads = listOf(ad("a1", 10.0, emptyList())))

                val result = mergeAvails(listOf(existing), listOf(incoming))

                expectThat(result.single().ads.single().trackingEvents).hasSize(3)
            }
        }
    }

    describe("withNextToken") {
        val url = "https://example.com/v1/tracking/hash/config/session"
        val base64Token = "UFQxSDQxTTMyLjQ5MDAwMVNfMjAyNi0xMC0zMFQwMTo1OTozMi44MzA1OTA3MTlaXzE="
        val expected = "$url?aws.nextToken=UFQxSDQxTTMyLjQ5MDAwMVNfMjAyNi0xMC0zMFQwMTo1OTozMi44MzA1OTA3MTlaXzE%3D"

        it("encodes a raw token once") {
            expectThat(url.withNextToken(base64Token)).isEqualTo(expected)
        }

        it("does not encode an already encoded token twice") {
            expectThat(url.withNextToken(base64Token.replace("=", "%3D"))).isEqualTo(expected)
        }

        it("encodes base64 special characters and keeps a literal plus") {
            expectThat(url.withNextToken("ab+c/d=="))
                .isEqualTo("$url?aws.nextToken=ab%2Bc%2Fd%3D%3D")
            expectThat(url.withNextToken("ab%2Bc%2Fd%3D%3D"))
                .isEqualTo("$url?aws.nextToken=ab%2Bc%2Fd%3D%3D")
        }

        it("preserves existing query parameters") {
            expectThat("$url?foo=bar".withNextToken("abc=")).isEqualTo("$url?foo=bar&aws.nextToken=abc%3D")
        }
    }
})

private fun avail(id: String, start: Double, duration: Double = 30.0, ads: List<Ad> = emptyList()) = Avail(
    ads = ads,
    availId = id,
    duration = "PT${duration}S",
    durationInSeconds = duration,
    startTime = "PT${start}S",
    startTimeInSeconds = start,
    adMarkerDuration = "PT${duration}S",
)

private fun ad(id: String, start: Double, trackingEvents: List<TrackingEvent>) = Ad(
    adId = id,
    duration = "PT15S",
    durationInSeconds = 15.0,
    startTime = "PT${start}S",
    startTimeInSeconds = start,
    trackingEvents = trackingEvents,
)

private fun event(eventId: String, eventType: String, beaconUrl: String, startTime: Double = 10.0) = TrackingEvent(
    beaconUrls = listOf(beaconUrl),
    duration = "PT0S",
    durationInSeconds = 0.0,
    eventId = eventId,
    eventType = eventType,
    startTime = "PT${startTime}S",
    startTimeInSeconds = startTime,
)

private fun events(eventId: String, count: Int) = (1..count).map { event(eventId, "type$it", "http://x/$eventId/$it") }
