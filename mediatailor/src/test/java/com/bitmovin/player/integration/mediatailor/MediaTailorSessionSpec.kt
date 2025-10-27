package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.integration.mediatailor.api.LinearAssetTypeConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAssetType
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult
import com.bitmovin.player.integration.mediatailor.network.HttpClient
import com.bitmovin.player.integration.mediatailor.network.HttpRequestResult
import com.bitmovin.player.integration.mediatailor.util.eventFlow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEmpty
import strikt.assertions.isTrue
import util.TestHttpClient
import util.mockkPlayerExtension
import util.unmockkPlayerExtension

@OptIn(ExperimentalCoroutinesApi::class)
class MediaTailorSessionSpec : DescribeSpec({
    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var mediaTailorSession: MediaTailorSession
    lateinit var adsMapper: AdsMapper

    fun setUp(player: Player = mockk(relaxed = true), httpClient: HttpClient = TestHttpClient()) {
        adsMapper = DefaultAdsMapper()
        mediaTailorSession = DefaultMediaTailorSession(
            player = player,
            httpClient = httpClient,
            adsMapper = adsMapper,
        )
    }

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
        mockkPlayerExtension()
    }

    afterSpec {
        Dispatchers.resetMain()
        unmockkPlayerExtension()
    }

    beforeEach {
        setUp()
    }

    describe("initializing a session") {
        describe("when the post request is successful") {
            beforeEach {
                setUp(
                    httpClient = mockk {
                        coEvery { post(any(), any()) } returns HttpRequestResult.Success(
                            body = successfulSessionInitResponse,
                        )
                    },
                )
            }

            it("returns a successful SessionInitializationResult") {
                val result = mediaTailorSession.initialize(
                    MediaTailorSessionConfig(
                        sessionInitUrl = TEST_SESSION_INIT_URL,
                        assetType = MediaTailorAssetType.Vod,
                    ),
                )

                expectThat(result)
                    .isA<SessionInitializationResult.Success>()
                    .and { get { manifestUrl }.isEqualTo(FAKE_MANIFEST_URL) }
            }

            it("isInitialized is true") {
                mediaTailorSession.initialize(
                    MediaTailorSessionConfig(
                        sessionInitUrl = TEST_SESSION_INIT_URL,
                        assetType = MediaTailorAssetType.Vod,
                    ),
                )

                expectThat(mediaTailorSession.isInitialized).isTrue()
            }

            describe("when the player is loaded") {
                val loadedFlow = MutableSharedFlow<SourceEvent.Loaded>()

                beforeEach {
                    val httpClient = mockk<HttpClient>()
                    coEvery { httpClient.post(TEST_SESSION_INIT_URL) } returns HttpRequestResult.Success(
                        body = successfulSessionInitResponse,
                    )
                    coEvery { httpClient.get(FAKE_TRACKING_URL) } returns HttpRequestResult.Success(
                        body = successfulTrackingResponse,
                    )
                    val player = mockk<Player>(relaxed = true)
                    every { player.eventFlow<SourceEvent.Loaded>() } returns loadedFlow
                    setUp(
                        player = player,
                        httpClient = httpClient,
                    )
                }

                it("does not request ad tracking url before loading") {
                    mediaTailorSession.initialize(
                        MediaTailorSessionConfig(
                            sessionInitUrl = TEST_SESSION_INIT_URL,
                            assetType = MediaTailorAssetType.Vod,
                        ),
                    )

                    expectThat(mediaTailorSession.adBreaks.value).isEmpty()
                }

                it("requests the ad tracking url") {
                    mediaTailorSession.initialize(
                        MediaTailorSessionConfig(
                            sessionInitUrl = TEST_SESSION_INIT_URL,
                            assetType = MediaTailorAssetType.Vod,
                        ),
                    )

                    loadedFlow.emit(SourceEvent.Loaded(source = mockk()))

                    expectThat(mediaTailorSession.adBreaks.value).isNotEmpty()
                }

                describe("when no avails are returned") {
                    val httpClient = mockk<HttpClient>()
                    beforeEach {
                        coEvery { httpClient.post(TEST_SESSION_INIT_URL) } returns HttpRequestResult.Success(
                            body = successfulSessionInitResponse,
                        )
                        coEvery { httpClient.get(FAKE_TRACKING_URL) } returns HttpRequestResult.Success(
                            body = emptyTrackingResponse,
                        )
                        val player = mockk<Player>(relaxed = true)
                        every { player.eventFlow<SourceEvent.Loaded>() } returns loadedFlow
                        every { player.isPlaying } returns true
                        setUp(
                            player = player,
                            httpClient = httpClient,
                        )
                    }

                    it("adbreak should be empty") {
                        val result = mediaTailorSession.initialize(
                            MediaTailorSessionConfig(
                                sessionInitUrl = TEST_SESSION_INIT_URL,
                                assetType = MediaTailorAssetType.Vod,
                            ),
                        )

                        loadedFlow.emit(SourceEvent.Loaded(source = mockk()))

                        expectThat(mediaTailorSession.adBreaks.value).isEmpty()
                    }
                }

                describe("when the asset type is linear") {
                    val httpClient = mockk<HttpClient>()
                    beforeEach {
                        coEvery { httpClient.post(TEST_SESSION_INIT_URL) } returns HttpRequestResult.Success(
                            body = successfulSessionInitResponse,
                        )
                        coEvery { httpClient.get(FAKE_TRACKING_URL) } returns HttpRequestResult.Success(
                            body = successfulTrackingResponse,
                        )
                        val player = mockk<Player>(relaxed = true)
                        every { player.eventFlow<SourceEvent.Loaded>() } returns loadedFlow
                        every { player.isPlaying } returns true
                        setUp(
                            player = player,
                            httpClient = httpClient,
                        )
                    }

                    it("polls the tracking url at the configured interval") {
                        mediaTailorSession.initialize(
                            MediaTailorSessionConfig(
                                sessionInitUrl = TEST_SESSION_INIT_URL,
                                assetType = MediaTailorAssetType.Linear(
                                    config = LinearAssetTypeConfig(
                                        trackingRequestPollFrequency = 2.seconds,
                                    ),
                                ),
                            ),
                        )

                        loadedFlow.emit(SourceEvent.Loaded(source = mockk()))

                        // Initial request after loading
                        coVerify(exactly = 1) { httpClient.get(FAKE_TRACKING_URL) }

                        testDispatcher.scheduler.advanceTimeBy(5.seconds.inWholeMilliseconds)

                        // Subsequent 2 polls expected after 5 seconds
                        coVerify(exactly = 3) { httpClient.get(FAKE_TRACKING_URL) }
                    }
                }
            }
        }

        describe("when the session init request fails") {
            beforeEach {
                setUp(
                    httpClient = mockk {
                        coEvery { post(any(), any()) } returns HttpRequestResult.Failure()
                    },
                )
            }

            it("returns a failed SessionInitializationResult") {
                val result = mediaTailorSession.initialize(
                    MediaTailorSessionConfig(
                        sessionInitUrl = TEST_SESSION_INIT_URL,
                        assetType = MediaTailorAssetType.Vod,
                    ),
                )

                expectThat(result).isA<SessionInitializationResult.Failure>()
            }

            it("isInitialized is false") {
                mediaTailorSession.initialize(
                    MediaTailorSessionConfig(
                        sessionInitUrl = TEST_SESSION_INIT_URL,
                        assetType = MediaTailorAssetType.Vod,
                    ),
                )

                expectThat(mediaTailorSession.isInitialized).isFalse()
            }
        }
    }
})

private const val TEST_SESSION_INIT_URL = "https://example.com/session-init"
private const val FAKE_TRACKING_URL = "https://example.com/tracking"
private const val FAKE_MANIFEST_URL = "https://example.com/manifest.m3u8"

private val successfulSessionInitResponse = """{
    "manifestUrl": "$FAKE_MANIFEST_URL",
    "trackingUrl": "$FAKE_TRACKING_URL"
} 
""".trimIndent()

private val successfulTrackingResponse = """
{
  "avails": [
    {
      "ads": [
        {
          "adId": "8104385",
          "duration": "PT15.100000078S",
          "durationInSeconds": 15.1,
          "startTime": "PT17.817798612S",
          "startTimeInSeconds": 17.817,
          "trackingEvents": [
          {
              "beaconUrls": [
                "http://exampleadserver.com/tracking?event=impression"
              ],
              "duration": "PT15.100000078S",
              "durationInSeconds": 15.1,
              "eventId": "8104385",
              "eventType": "impression",
              "startTime": "PT17.817798612S",
              "startTimeInSeconds": 17.817
            },
            {
              "beaconUrls": [
                "http://exampleadserver.com/tracking?event=start"
              ],
              "duration": "PT0S",
              "durationInSeconds": 0.0,
              "eventId": "8104385",
              "eventType": "start",
              "startTime": "PT17.817798612S",
              "startTimeInSeconds": 17.817
            },
            {
              "beaconUrls": [
                "http://exampleadserver.com/tracking?event=firstQuartile"
              ],
              "duration": "PT0S",
              "durationInSeconds": 0.0,
              "eventId": "8104386",
              "eventType": "firstQuartile",
              "startTime": "PT21.592798631S",
              "startTimeInSeconds": 21.592
            },
             {
              "beaconUrls": [
                "http://exampleadserver.com/tracking?event=midpoint"
              ],
              "duration": "PT0S",
              "durationInSeconds": 0.0,
              "eventId": "8104387",
              "eventType": "midpoint",
              "startTime": "PT25.367798651S",
              "startTimeInSeconds": 25.367
            },
            {
              "beaconUrls": [
                "http://exampleadserver.com/tracking?event=thirdQuartile"
              ],
              "duration": "PT0S",
              "durationInSeconds": 0.0,
              "eventId": "8104388",
              "eventType": "thirdQuartile",
              "startTime": "PT29.14279867S",
              "startTimeInSeconds": 29.142
            },
            {
              "beaconUrls": [
                "http://exampleadserver.com/tracking?event=complete"
              ],
              "duration": "PT0S",
              "durationInSeconds": 0.0,
              "eventId": "8104390",
              "eventType": "complete",
              "startTime": "PT32.91779869S",
              "startTimeInSeconds": 32.917
            }
          ]
        }
      ],
      "availId": "8104385",
      "duration": "PT15.100000078S",
      "durationInSeconds": 15.1,
      "adMarkerDuration": "PT15.100000078S",
      "startTime": "PT17.817798612S",
      "startTimeInSeconds": 17.817
    }
  ]
}
""".trimIndent()

private val emptyTrackingResponse = """
    {
    "avails": [
        {
            "adBreakTrackingEvents": [
                {
                    "beaconUrls": [
                        "https://pubads.g.doubleclick.net/pagead/interaction/?ai=BBkdcvoL4aNovsO71_A_T2YrpAqb3vIhHAAAAEAEg_rO1YzgBWOPc4PyDBGClgICAkAG6AQlnZnBfaW1hZ2XIAQWpAg-xrj8UxqU-wAIC4AIA6gIiLzIxNjgxMjAxMzQwL3N0YW4vYXBwL2FuZHJvaWQvdm9kL_gC_NEegAMBkAPkCpgD5AqoAwHgBAHSBQYQhcys3xmQBgGgBiPYBgSoB7i-sQKoB_PRG6gHltgbqAeqm7ECqAeaBqgHl8WxAqgH_56xAqgH35-xAqgH-MKxAqgH-8KxAtgHAeAHAdIIOAiM4YBAEAEYnQEyCIvCge6fgI0IOhSCwICAgICMqAGAgICAgICogAKoA0i9_cE6WM2f3Meft5AD2AgCgAoFmAsBqg0CQVXqDRMIza7fx5-3kAMVMHedCR3TrCIt0BUB-BYBgBcB&sig..."
                    ],
                    "eventType": "breakEnd"
                },
                {
                    "beaconUrls": [
                        "https://pubads.g.doubleclick.net/pagead/interaction/?ai=BBkdcvoL4aNovsO71_A_T2YrpAqb3vIhHAAAAEAEg_rO1YzgBWOPc4PyDBGClgICAkAG6AQlnZnBfaW1hZ2XIAQWpAg-xrj8UxqU-wAIC4AIA6gIiLzIxNjgxMjAxMzQwL3N0YW4vYXBwL2FuZHJvaWQvdm9kL_gC_NEegAMBkAPkCpgD5AqoAwHgBAHSBQYQhcys3xmQBgGgBiPYBgSoB7i-sQKoB_PRG6gHltgbqAeqm7ECqAeaBqgHl8WxAqgH_56xAqgH35-xAqgH-MKxAqgH-8KxAtgHAeAHAdIIOAiM4YBAEAEYnQEyCIvCge6fgI0IOhSCwICAgICMqAGAgICAgICogAKoA0i9_cE6WM2f3Meft5AD2AgCgAoFmAsBqg0CQVXqDRMIza7fx5-3kAMVMHedCR3TrCIt0BUB-BYBgBcB&sig..."
                    ],
                    "eventType": "breakStart"
                }
            ],
            "adMarkerDuration": "PT0S",
            "ads": [],
            "availId": "0",
            "availProgramDateTime": null,
            "duration": "PT0S",
            "durationInSeconds": 0.0,
            "meta": null,
            "nonLinearAdsList": [],
            "startTime": "PT0S",
            "startTimeInSeconds": 0.0
        }
    ],
    "dashAvailabilityStartTime": null,
    "hlsAnchorMediaSequenceNumber": 0,
    "nextToken": null,
    "nonLinearAvails": []
}""".trimIndent()
