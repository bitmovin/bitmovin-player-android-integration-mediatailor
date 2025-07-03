package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAssetType
import com.bitmovin.player.integration.mediatailor.api.MediaTailorLinearAd
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorTrackingEvent
import com.bitmovin.player.integration.mediatailor.util.eventFlow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsSequence
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.single
import util.TestAdPlaybackTracker
import util.TestHttpClient
import util.mockkPlayerExtension
import util.unmockkPlayerExtension

@OptIn(ExperimentalCoroutinesApi::class)
class AdBeaconingSpec : DescribeSpec({
    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var testHttpClient: TestHttpClient
    lateinit var adBeaconing: AdBeaconing
    lateinit var adPlaybackTracker: TestAdPlaybackTracker
    lateinit var playingAdBreakFlow: MutableStateFlow<PlayingAdBreak?>

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
        mockkPlayerExtension()
    }

    afterSpec {
        Dispatchers.resetMain()
        unmockkPlayerExtension()
    }

    fun setUp(player: Player = mockk(relaxed = true), playingAdBreak: PlayingAdBreak? = null, automaticTrackingEnables: Boolean = true) {
        val sessionConfig = MediaTailorSessionConfig(
            sessionInitUrl = "http://example.com/session",
            assetType = MediaTailorAssetType.Vod,
            automaticAdTrackingEnabled = automaticTrackingEnables,
        )
        playingAdBreakFlow = MutableStateFlow(playingAdBreak)
        testHttpClient = TestHttpClient()
        adPlaybackTracker = TestAdPlaybackTracker(
            nextAdBreak = MutableStateFlow(null),
            playingAdBreak = playingAdBreakFlow,
        )
        adBeaconing = DefaultAdBeaconing(
            player = player,
            adPlaybackTracker = adPlaybackTracker,
            httpClient = testHttpClient,
            eventEmitter = mockk(relaxed = true),
            sessionConfig = sessionConfig,
        )
    }

    beforeEach {
        setUp()
    }

    describe("tracking linear ad metric events") {
        val timeChangedFlow = MutableSharedFlow<PlayerEvent.TimeChanged>()
        beforeEach {
            setUp(
                player = mockk(relaxed = true) {
                    every { eventFlow<PlayerEvent.TimeChanged>() } returns timeChangedFlow
                },
            )
        }

        describe("and an ad is not playing") {
            it("does not send any tracking events") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(0.0))

                expectThat(testHttpClient.getRequests).isEmpty()
            }
        }

        describe("and an ad is playing") {
            val playingAdBreak = PlayingAdBreak(
                adBreak = MediaTailorAdBreak(
                    id = "adBreak1",
                    ads = listOf(
                        MediaTailorLinearAd(
                            id = "ad1",
                            scheduleTime = 0.0,
                            duration = 10.0,
                            formattedDuration = "10",
                            trackingEvents = listOf(
                                MediaTailorTrackingEvent(
                                    id = "start",
                                    scheduleTime = 0.0,
                                    duration = 0.0,
                                    eventType = "start",
                                    beaconUrls = listOf("http://example.com/start"),
                                ),
                                MediaTailorTrackingEvent(
                                    id = "firstQuartile",
                                    scheduleTime = 2.5,
                                    duration = 0.0,
                                    eventType = "firstQuartile",
                                    beaconUrls = listOf("http://example.com/firstQuartile"),
                                ),
                                MediaTailorTrackingEvent(
                                    id = "midpoint",
                                    scheduleTime = 5.0,
                                    duration = 0.0,
                                    eventType = "midpoint",
                                    beaconUrls = listOf("http://example.com/midpoint"),
                                ),
                                MediaTailorTrackingEvent(
                                    id = "thirdQuartile",
                                    scheduleTime = 7.5,
                                    duration = 0.0,
                                    eventType = "thirdQuartile",
                                    beaconUrls = listOf("http://example.com/thirdQuartile"),
                                ),
                                MediaTailorTrackingEvent(
                                    id = "complete",
                                    scheduleTime = 10.0,
                                    duration = 0.0,
                                    eventType = "complete",
                                    beaconUrls = listOf("http://example.com/complete"),
                                ),
                                MediaTailorTrackingEvent(
                                    id = "nonLinear",
                                    scheduleTime = 11.0,
                                    duration = 0.0,
                                    eventType = "nonLinear",
                                    beaconUrls = listOf("http://example.com/nonLinear"),
                                ),
                                MediaTailorTrackingEvent(
                                    id = "impression",
                                    scheduleTime = -1.0,
                                    duration = 0.0,
                                    eventType = "impression",
                                    beaconUrls = listOf("http://example.com/impression"),
                                ),
                            ),
                        ),
                    ),
                    scheduleTime = 0.0,
                    duration = 10.0,
                    formattedDuration = "10",
                    adMarkerDuration = "10",
                ),
                adIndex = 0,
            )
            beforeEach {
                setUp(
                    player = mockk(relaxed = true) {
                        every { eventFlow<PlayerEvent.TimeChanged>() } returns timeChangedFlow
                    },
                    playingAdBreak = playingAdBreak,
                )
            }

            it("sends start event") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(0.0))

                expectThat(testHttpClient.getRequests).contains("http://example.com/start")
            }

            it("sends first quartile event") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(2.5))

                expectThat(testHttpClient.getRequests).contains("http://example.com/firstQuartile")
            }

            it("sends midpoint event") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(5.0))

                expectThat(testHttpClient.getRequests).contains("http://example.com/midpoint")
            }

            it("sends third quartile event") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(7.5))

                expectThat(testHttpClient.getRequests).contains("http://example.com/thirdQuartile")
            }

            it("sends complete event") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(10.0))

                expectThat(testHttpClient.getRequests).contains("http://example.com/complete")
            }

            it("does not send non-linear tracking events") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(11.0))

                expectThat(testHttpClient.getRequests).isEmpty()
            }

            it("sends tracking events only once") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(-1.0))
                timeChangedFlow.emit(PlayerEvent.TimeChanged(-1.0))

                expectThat(testHttpClient.getRequests).single()
                    .isEqualTo("http://example.com/impression")
            }

            describe("when the playing ad break changes") {
                it("sends same tracking event again") {
                    timeChangedFlow.emit(PlayerEvent.TimeChanged(-1.0))
                    expectThat(testHttpClient.getRequests).single()
                        .isEqualTo("http://example.com/impression")
                    testHttpClient.clear()
                    expectThat(testHttpClient.getRequests).isEmpty()

                    playingAdBreakFlow.update { null }

                    timeChangedFlow.emit(PlayerEvent.TimeChanged(-1.0))
                    expectThat(testHttpClient.getRequests).isEmpty()

                    playingAdBreakFlow.update { playingAdBreak }

                    timeChangedFlow.emit(PlayerEvent.TimeChanged(-1.0))
                    expectThat(testHttpClient.getRequests).single()
                        .isEqualTo("http://example.com/impression")
                }
            }

            describe("when automatic ad tracking is disabled") {
                beforeEach {
                    setUp(
                        player = mockk(relaxed = true) {
                            every { eventFlow<PlayerEvent.TimeChanged>() } returns timeChangedFlow
                        },
                        playingAdBreak = playingAdBreak,
                        automaticTrackingEnables = false,
                    )
                }

                it("does not send any tracking events") {
                    timeChangedFlow.emit(PlayerEvent.TimeChanged(0.0))

                    expectThat(testHttpClient.getRequests).isEmpty()
                }
            }
        }
    }

    describe("tracking player events") {
        describe("when there is no playing ad") {
            it("does not send any tracking events") {
                expectThat(testHttpClient.getRequests).isEmpty()
            }
        }

        describe("when there is a playing ad") {
            val playingAdBreak = PlayingAdBreak(
                adBreak = MediaTailorAdBreak(
                    id = "adBreak1",
                    ads = listOf(
                        MediaTailorLinearAd(
                            id = "ad1",
                            scheduleTime = 0.0,
                            duration = 10.0,
                            formattedDuration = "10",
                            trackingEvents = listOf(
                                MediaTailorTrackingEvent(
                                    id = "pause",
                                    scheduleTime = 0.0,
                                    duration = 0.0,
                                    eventType = "pause",
                                    beaconUrls = listOf("http://example.com/pause"),
                                ),
                                MediaTailorTrackingEvent(
                                    id = "resume",
                                    scheduleTime = 0.0,
                                    duration = 0.0,
                                    eventType = "resume",
                                    beaconUrls = listOf(
                                        "http://example.com/resume",
                                        "http://example.com/resume2",
                                    ),
                                ),
                            ),
                        ),
                    ),
                    scheduleTime = 0.0,
                    duration = 10.0,
                    formattedDuration = "10",
                    adMarkerDuration = "10",
                ),
                adIndex = 0,
            )
            beforeEach {
                setUp(
                    playingAdBreak = playingAdBreak,
                )
            }

            describe("and the ad contains the tracking type") {
                it("sends the tracking event for the type") {
                    adBeaconing.track("pause")

                    expectThat(testHttpClient.getRequests).contains("http://example.com/pause")
                }

                it("sends all tracking events for the type") {
                    adBeaconing.track("resume")

                    expectThat(testHttpClient.getRequests)
                        .containsSequence("http://example.com/resume", "http://example.com/resume2")
                }

                describe("and automatic ad tracking is disabled") {
                    beforeEach {
                        setUp(
                            playingAdBreak = playingAdBreak,
                            automaticTrackingEnables = false,
                        )
                    }

                    it("sends all tracking events for the type") {
                        adBeaconing.track("resume")

                        expectThat(testHttpClient.getRequests)
                            .containsSequence("http://example.com/resume", "http://example.com/resume2")
                    }
                }
            }

            describe("and the ad does not contain the tracking type") {
                it("does not send any tracking events") {
                    adBeaconing.track("exitFullscreen")

                    expectThat(testHttpClient.getRequests).isEmpty()
                }
            }
        }

        describe("player events trigger tracking events") {
            val mutedFlow = MutableSharedFlow<PlayerEvent.Muted>()
            val unmutedFlow = MutableSharedFlow<PlayerEvent.Unmuted>()
            val playingFlow = MutableSharedFlow<PlayerEvent.Play>()
            val pausedFlow = MutableSharedFlow<PlayerEvent.Paused>()
            val fullscreenEnteredFlow = MutableSharedFlow<PlayerEvent.FullscreenEnter>()
            val fullscreenExitedFlow = MutableSharedFlow<PlayerEvent.FullscreenExit>()

            val player = mockk<Player>(relaxed = true) {
                every { eventFlow<PlayerEvent.Muted>() } returns mutedFlow
                every { eventFlow<PlayerEvent.Unmuted>() } returns unmutedFlow
                every { eventFlow<PlayerEvent.Play>() } returns playingFlow
                every { eventFlow<PlayerEvent.Paused>() } returns pausedFlow
                every { eventFlow<PlayerEvent.FullscreenEnter>() } returns fullscreenEnteredFlow
                every { eventFlow<PlayerEvent.FullscreenExit>() } returns fullscreenExitedFlow
            }

            val playingAdBreak = PlayingAdBreak(
                adBreak = MediaTailorAdBreak(
                    id = "adBreak1",
                    ads = listOf(
                        MediaTailorLinearAd(
                            id = "ad1",
                            scheduleTime = 0.0,
                            duration = 10.0,
                            formattedDuration = "10",
                            trackingEvents = listOf(
                                MediaTailorTrackingEvent(
                                    id = "mute",
                                    scheduleTime = 0.0,
                                    duration = 0.0,
                                    eventType = "mute",
                                    beaconUrls = listOf("http://example.com/mute"),
                                ),
                                MediaTailorTrackingEvent(
                                    id = "unmute",
                                    scheduleTime = 0.0,
                                    duration = 0.0,
                                    eventType = "unmute",
                                    beaconUrls = listOf("http://example.com/unmute"),
                                ),
                                MediaTailorTrackingEvent(
                                    id = "resume",
                                    scheduleTime = 0.0,
                                    duration = 0.0,
                                    eventType = "resume",
                                    beaconUrls = listOf("http://example.com/play"),
                                ),
                                MediaTailorTrackingEvent(
                                    id = "pause",
                                    scheduleTime = 0.0,
                                    duration = 0.0,
                                    eventType = "pause",
                                    beaconUrls = listOf("http://example.com/pause"),
                                ),
                                MediaTailorTrackingEvent(
                                    id = "fullscreen",
                                    scheduleTime = 0.0,
                                    duration = 0.0,
                                    eventType = "fullscreen",
                                    beaconUrls = listOf("http://example.com/fullscreenEnter"),
                                ),
                                MediaTailorTrackingEvent(
                                    id = "exitFullscreen",
                                    scheduleTime = 0.0,
                                    duration = 0.0,
                                    eventType = "exitFullscreen",
                                    beaconUrls = listOf("http://example.com/fullscreenExit"),
                                ),
                            ),
                        ),
                    ),
                    scheduleTime = 0.0,
                    duration = 10.0,
                    formattedDuration = "10",
                    adMarkerDuration = "10",
                ),
                adIndex = 0,
            )

            beforeEach {
                setUp(
                    player = player,
                    playingAdBreak = playingAdBreak,
                )
            }

            it("sends tracking event on mute") {
                mutedFlow.emit(PlayerEvent.Muted())

                expectThat(testHttpClient.getRequests).contains("http://example.com/mute")
            }

            it("sends tracking event on unmute") {
                unmutedFlow.emit(PlayerEvent.Unmuted())

                expectThat(testHttpClient.getRequests).contains("http://example.com/unmute")
            }

            it("sends tracking event on play") {
                playingFlow.emit(PlayerEvent.Play(time = 0.0))

                expectThat(testHttpClient.getRequests).contains("http://example.com/play")
            }

            it("sends tracking event on pause") {
                pausedFlow.emit(PlayerEvent.Paused(time = 0.0))

                expectThat(testHttpClient.getRequests).contains("http://example.com/pause")
            }

            it("sends tracking event on fullscreen enter") {
                fullscreenEnteredFlow.emit(PlayerEvent.FullscreenEnter())

                expectThat(testHttpClient.getRequests).contains("http://example.com/fullscreenEnter")
            }

            it("sends tracking event on fullscreen exit") {
                fullscreenExitedFlow.emit(PlayerEvent.FullscreenExit())

                expectThat(testHttpClient.getRequests).contains("http://example.com/fullscreenExit")
            }

            describe("and automatic ad tracking is disabled") {
                beforeEach {
                    setUp(
                        player = player,
                        playingAdBreak = playingAdBreak,
                        automaticTrackingEnables = false,
                    )
                }

                it("does not send any tracking events") {
                    mutedFlow.emit(PlayerEvent.Muted())
                    unmutedFlow.emit(PlayerEvent.Unmuted())
                    playingFlow.emit(PlayerEvent.Play(time = 0.0))
                    pausedFlow.emit(PlayerEvent.Paused(time = 0.0))
                    fullscreenEnteredFlow.emit(PlayerEvent.FullscreenEnter())
                    fullscreenExitedFlow.emit(PlayerEvent.FullscreenExit())

                    expectThat(testHttpClient.getRequests).isEmpty()
                }
            }
        }
    }
})
