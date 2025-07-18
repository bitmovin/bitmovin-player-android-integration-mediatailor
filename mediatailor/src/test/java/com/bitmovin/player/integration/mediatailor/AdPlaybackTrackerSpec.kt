@file:OptIn(ExperimentalCoroutinesApi::class)

package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.api.MediaTailorLinearAd
import com.bitmovin.player.integration.mediatailor.util.eventFlow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import util.TestMediaTailorSession
import util.mockkPlayerExtension
import util.unmockkPlayerExtension

class AdPlaybackTrackerSpec : DescribeSpec({
    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var player: Player
    lateinit var adPlaybackTracker: AdPlaybackTracker

    fun createPlayer(
        currentTime: Double = 0.0,
        timeChangedFlow: MutableSharedFlow<PlayerEvent.TimeChanged>? = null,
        timeShiftedFlow: MutableSharedFlow<PlayerEvent.TimeShifted>? = null,
        seekedFlow: MutableSharedFlow<PlayerEvent.Seeked>? = null,
    ) {
        player = mockk<Player>(relaxed = true)
        every { player.currentTime } returns currentTime
        timeChangedFlow?.let {
            every { player.eventFlow<PlayerEvent.TimeChanged>() } returns timeChangedFlow
        }
        timeShiftedFlow?.let {
            every { player.eventFlow<PlayerEvent.TimeShifted>() } returns timeShiftedFlow
        }
        seekedFlow?.let {
            every { player.eventFlow<PlayerEvent.Seeked>() } returns seekedFlow
        }
    }

    fun createAdPlaybackTracker(adBreaks: List<MediaTailorAdBreak> = emptyList()) {
        val fakeMediaTailorSession = TestMediaTailorSession(
            adBreaks = MutableStateFlow(adBreaks),
        )

        adPlaybackTracker = DefaultAdPlaybackTracker(
            player,
            fakeMediaTailorSession,
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
        createPlayer()
        createAdPlaybackTracker()
    }

    describe("initializing the ad playback tracker") {
        describe("with no ad breaks in the session") {
            it("currentAdBreak is null") {
                expectThat(adPlaybackTracker.playingAdBreak.value).isNull()
            }

            it("nextAdBreak is null") {
                expectThat(adPlaybackTracker.nextAdBreak.value).isNull()
            }
        }

        describe("with an ad break in the present") {
            lateinit var adBreak: MediaTailorAdBreak
            beforeEach {
                createPlayer(currentTime = 5.0)
                adBreak = MediaTailorAdBreak(
                    id = "adBreak1",
                    ads = listOf(
                        MediaTailorLinearAd(
                            id = "ad1",
                            scheduleTime = 4.0,
                            duration = 5.0,
                            formattedDuration = "5",
                            trackingEvents = listOf(),
                        ),
                    ),
                    scheduleTime = 4.0,
                    duration = 5.0,
                    formattedDuration = "5",
                    adMarkerDuration = "5",
                )
                createAdPlaybackTracker(listOf(adBreak))
            }

            it("currentAdBreak is the correct adBreak") {
                expectThat(adPlaybackTracker.playingAdBreak.value?.adBreak).isEqualTo(adBreak)
            }
        }

        describe("with an ad break in the past") {
            lateinit var adBreak: MediaTailorAdBreak
            beforeEach {
                createPlayer(currentTime = 5.0)
                adBreak = MediaTailorAdBreak(
                    id = "adBreak1",
                    ads = listOf(),
                    scheduleTime = 0.0,
                    duration = 4.0,
                    formattedDuration = "4",
                    adMarkerDuration = "4",
                )
                createAdPlaybackTracker(listOf(adBreak))
            }

            it("currentAdBreak is null") {
                expectThat(adPlaybackTracker.playingAdBreak.value).isNull()
            }
        }

        describe("with an ad break in the future") {
            lateinit var adBreak: MediaTailorAdBreak
            beforeEach {
                createPlayer(currentTime = 5.0)
                adBreak = MediaTailorAdBreak(
                    id = "adBreak1",
                    ads = listOf(),
                    scheduleTime = 10.0,
                    duration = 5.0,
                    formattedDuration = "5",
                    adMarkerDuration = "5",
                )
                createAdPlaybackTracker(listOf(adBreak))
            }

            it("currentAdBreak is null") {
                expectThat(adPlaybackTracker.playingAdBreak.value).isNull()
            }

            it("nextAdBreak is the correct adBreak") {
                expectThat(adPlaybackTracker.nextAdBreak.value).isEqualTo(adBreak)
            }
        }

        describe("with ad break in the past future and current ad break") {
            lateinit var currentAdBreak: MediaTailorAdBreak
            lateinit var nextAdBreak: MediaTailorAdBreak
            beforeEach {
                createPlayer(currentTime = 5.0)
                currentAdBreak = MediaTailorAdBreak(
                    id = "adBreak1",
                    ads = listOf(
                        MediaTailorLinearAd(
                            id = "ad1",
                            scheduleTime = 4.0,
                            duration = 3.0,
                            formattedDuration = "3",
                            trackingEvents = listOf(),
                        ),
                    ),
                    scheduleTime = 4.0,
                    duration = 3.0,
                    formattedDuration = "3",
                    adMarkerDuration = "3",
                )
                nextAdBreak = MediaTailorAdBreak(
                    id = "adBreak2",
                    ads = listOf(
                        MediaTailorLinearAd(
                            id = "ad2",
                            scheduleTime = 10.0,
                            duration = 5.0,
                            formattedDuration = "5",
                            trackingEvents = listOf(),
                        ),
                    ),
                    scheduleTime = 10.0,
                    duration = 5.0,
                    formattedDuration = "5",
                    adMarkerDuration = "5",
                )
                createAdPlaybackTracker(listOf(currentAdBreak, nextAdBreak))
            }

            it("currentAdBreak is the correct adBreak") {
                expectThat(adPlaybackTracker.playingAdBreak.value?.adBreak).isEqualTo(currentAdBreak)
            }

            it("nextAdBreak is the correct adBreak") {
                expectThat(adPlaybackTracker.nextAdBreak.value).isEqualTo(nextAdBreak)
            }
        }

        describe("with multiple ads in the ad break") {
            val firstAd = MediaTailorLinearAd(
                id = "ad1",
                scheduleTime = 5.0,
                duration = 5.0,
                formattedDuration = "5",
                trackingEvents = listOf(),
            )
            val secondAd = MediaTailorLinearAd(
                id = "ad2",
                scheduleTime = 10.0,
                duration = 5.0,
                formattedDuration = "5",
                trackingEvents = listOf(),
            )
            val thirdAd = MediaTailorLinearAd(
                id = "ad3",
                scheduleTime = 15.0,
                duration = 5.0,
                formattedDuration = "5",
                trackingEvents = listOf(),
            )
            val adBreak = MediaTailorAdBreak(
                id = "adBreak1",
                ads = listOf(firstAd, secondAd, thirdAd),
                scheduleTime = 5.0,
                duration = 15.0,
                formattedDuration = "15",
                adMarkerDuration = "15",
            )

            describe("when the player time is at first ad") {
                beforeEach {
                    createPlayer(currentTime = 5.0)
                    createAdPlaybackTracker(listOf(adBreak))
                }

                it("current ad is the first ad") {
                    expectThat(adPlaybackTracker.playingAdBreak.value?.ad).isEqualTo(firstAd)
                }

                it("returns the correct index") {
                    expectThat(adPlaybackTracker.playingAdBreak.value?.adIndex).isEqualTo(0)
                }
            }

            describe("when the player time is at second ad") {
                beforeEach {
                    createPlayer(currentTime = 10.0)
                    createAdPlaybackTracker(listOf(adBreak))
                }

                it("current ad is the second ad") {
                    expectThat(adPlaybackTracker.playingAdBreak.value?.ad).isEqualTo(secondAd)
                }

                it("returns the correct index") {
                    expectThat(adPlaybackTracker.playingAdBreak.value?.adIndex).isEqualTo(1)
                }
            }

            describe("when the player time is at third ad") {
                beforeEach {
                    createPlayer(currentTime = 15.0)
                    createAdPlaybackTracker(listOf(adBreak))
                }

                it("current ad is the third ad") {
                    expectThat(adPlaybackTracker.playingAdBreak.value?.ad).isEqualTo(thirdAd)
                }

                it("returns the correct index") {
                    expectThat(adPlaybackTracker.playingAdBreak.value?.adIndex).isEqualTo(2)
                }
            }
        }
    }

    describe("when player receives seeking and time shifting events") {
        val timeShiftedFlow = MutableSharedFlow<PlayerEvent.TimeShifted>()
        val seekedFlow = MutableSharedFlow<PlayerEvent.Seeked>()
        lateinit var firstAdBreak: MediaTailorAdBreak
        lateinit var secondAdBreak: MediaTailorAdBreak

        beforeEach {
            createPlayer(
                currentTime = 5.0,
                timeShiftedFlow = timeShiftedFlow,
                seekedFlow = seekedFlow,
            )
            firstAdBreak = MediaTailorAdBreak(
                id = "adBreak1",
                ads = listOf(
                    MediaTailorLinearAd(
                        id = "ad1",
                        scheduleTime = 4.0,
                        duration = 3.0,
                        formattedDuration = "3",
                        trackingEvents = listOf(),
                    ),
                ),
                scheduleTime = 4.0,
                duration = 3.0,
                formattedDuration = "3",
                adMarkerDuration = "3",
            )
            secondAdBreak = MediaTailorAdBreak(
                id = "adBreak2",
                ads = listOf(
                    MediaTailorLinearAd(
                        id = "ad1",
                        scheduleTime = 10.0,
                        duration = 5.0,
                        formattedDuration = "5",
                        trackingEvents = listOf(),
                    ),
                ),
                scheduleTime = 10.0,
                duration = 5.0,
                formattedDuration = "5",
                adMarkerDuration = "5",
            )
            createAdPlaybackTracker(listOf(firstAdBreak, secondAdBreak))
        }

        describe("before the time shift or seek") {
            it("current ad break is the first ad break") {
                expectThat(adPlaybackTracker.playingAdBreak.value?.adBreak).isEqualTo(firstAdBreak)
            }

            it("next ad break is the second ad break") {
                expectThat(adPlaybackTracker.nextAdBreak.value).isEqualTo(secondAdBreak)
            }
        }

        describe("after the time shift or seek") {
            beforeEach {
                // Time has to change otherwise the same ad breaks will be calculated again
                every { player.currentTime } returns 50.0
            }

            it("resets the state when time shifted") {
                timeShiftedFlow.emit(PlayerEvent.TimeShifted())

                expectThat(adPlaybackTracker.playingAdBreak.value).isEqualTo(null)
                expectThat(adPlaybackTracker.nextAdBreak.value).isEqualTo(null)
            }

            it("resets the state when seeked") {
                seekedFlow.emit(PlayerEvent.Seeked())

                expectThat(adPlaybackTracker.playingAdBreak.value).isEqualTo(null)
                expectThat(adPlaybackTracker.nextAdBreak.value).isEqualTo(null)
            }
        }
    }

    describe("when the player time progresses") {
        val timeChangedFlow = MutableSharedFlow<PlayerEvent.TimeChanged>()
        lateinit var firstAdBreak: MediaTailorAdBreak
        lateinit var secondAdBreak: MediaTailorAdBreak

        beforeEach {
            createPlayer(
                currentTime = 0.0,
                timeChangedFlow = timeChangedFlow,
            )
            firstAdBreak = MediaTailorAdBreak(
                id = "adBreak1",
                ads = listOf(
                    MediaTailorLinearAd(
                        id = "ad1",
                        scheduleTime = 4.0,
                        duration = 3.0,
                        formattedDuration = "3",
                        trackingEvents = listOf(),
                    ),
                ),
                scheduleTime = 4.0,
                duration = 3.0,
                formattedDuration = "3",
                adMarkerDuration = "3",
            )
            secondAdBreak = MediaTailorAdBreak(
                id = "adBreak2",
                ads = listOf(
                    MediaTailorLinearAd(
                        id = "ad1",
                        scheduleTime = 10.0,
                        duration = 5.0,
                        formattedDuration = "5",
                        trackingEvents = listOf(),
                    ),
                ),
                scheduleTime = 10.0,
                duration = 5.0,
                formattedDuration = "5",
                adMarkerDuration = "5",
            )
            createAdPlaybackTracker(listOf(firstAdBreak, secondAdBreak))
        }

        describe("and changes to 5.0") {
            beforeEach {
                every { player.currentTime } returns 5.0
            }

            it("correctly updates the current ad break and next ad break") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(5.0))

                expectThat(adPlaybackTracker.playingAdBreak.value?.adBreak).isEqualTo(firstAdBreak)
                expectThat(adPlaybackTracker.nextAdBreak.value).isEqualTo(secondAdBreak)
            }
        }

        describe("and changes to 10.0") {
            beforeEach {
                every { player.currentTime } returns 10.0
            }

            it("correctly updates the current ad break and next ad break") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(10.0))

                expectThat(adPlaybackTracker.playingAdBreak.value?.adBreak).isEqualTo(secondAdBreak)
                expectThat(adPlaybackTracker.nextAdBreak.value).isNull()
            }
        }

        describe("and changes to 16.0") {
            beforeEach {
                every { player.currentTime } returns 16.0
            }

            it("correctly updates the current ad break and next ad break") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(16.0))

                expectThat(adPlaybackTracker.playingAdBreak.value).isNull()
                expectThat(adPlaybackTracker.nextAdBreak.value).isNull()
            }
        }
    }

    describe("when the player moves from one ad break to another") {
        val timeChangedFlow = MutableSharedFlow<PlayerEvent.TimeChanged>()
        lateinit var firstAdBreak: MediaTailorAdBreak
        lateinit var secondAdBreak: MediaTailorAdBreak

        beforeEach {
            createPlayer(
                currentTime = 1.5,
                timeChangedFlow = timeChangedFlow,
            )
            firstAdBreak = MediaTailorAdBreak(
                id = "adBreak1",
                ads = listOf(
                    MediaTailorLinearAd(
                        id = "ad1",
                        scheduleTime = 0.0,
                        duration = 1.0,
                        formattedDuration = "1",
                        trackingEvents = listOf(),
                    ),
                    MediaTailorLinearAd(
                        id = "ad2",
                        scheduleTime = 1.0,
                        duration = 1.0,
                        formattedDuration = "1",
                        trackingEvents = listOf(),
                    ),
                ),
                scheduleTime = 0.0,
                duration = 2.0,
                formattedDuration = "2",
                adMarkerDuration = "2",
            )
            secondAdBreak = MediaTailorAdBreak(
                id = "adBreak2",
                ads = listOf(
                    MediaTailorLinearAd(
                        id = "ad1",
                        scheduleTime = 3.0,
                        duration = 1.0,
                        formattedDuration = "1",
                        trackingEvents = listOf(),
                    ),
                ),
                scheduleTime = 3.0,
                duration = 1.0,
                formattedDuration = "1",
                adMarkerDuration = "1",
            )
            createAdPlaybackTracker(listOf(firstAdBreak, secondAdBreak))
        }

        describe("when the player starts in the second ad in the first ad break") {
            it("selects the correct ad index initially") {
                expectThat(adPlaybackTracker.playingAdBreak.value?.adBreak).isEqualTo(firstAdBreak)
                expectThat(adPlaybackTracker.playingAdBreak.value?.adIndex).isEqualTo(1)
            }
        }

        describe("when the player moves directly into the second ad break with only one ad") {
            beforeEach {
                every { player.currentTime } returns 3.5
            }

            it("does not crash") {
                runTest {
                    timeChangedFlow.emit(PlayerEvent.TimeChanged(3.5))
                }
            }

            it("selects the first ad in the second ad break") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(3.5))

                expectThat(adPlaybackTracker.playingAdBreak.value?.adBreak).isEqualTo(secondAdBreak)
                expectThat(adPlaybackTracker.playingAdBreak.value?.adIndex).isEqualTo(0)
            }
        }
    }
})
