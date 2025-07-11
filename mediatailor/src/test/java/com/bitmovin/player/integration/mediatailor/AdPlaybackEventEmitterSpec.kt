package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent.UpcomingAdBreakUpdated
import com.bitmovin.player.integration.mediatailor.api.MediaTailorLinearAd
import io.kotest.core.spec.style.DescribeSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.containsSequence
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.single
import util.TestAdPlaybackTracker
import util.TestEventEmitter

@OptIn(ExperimentalCoroutinesApi::class)
class AdPlaybackEventEmitterSpec : DescribeSpec({
    val testDispatcher = UnconfinedTestDispatcher()
    val testEventEmitter = TestEventEmitter()
    lateinit var adPlaybackEventEmitter: AdPlaybackEventEmitter
    lateinit var nextAdBreak: MutableStateFlow<MediaTailorAdBreak?>
    lateinit var playingAdBreak: MutableStateFlow<PlayingAdBreak?>

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    beforeEach {
        nextAdBreak = MutableStateFlow<MediaTailorAdBreak?>(null)
        playingAdBreak = MutableStateFlow<PlayingAdBreak?>(null)
        val testAdPlaybackTracker = TestAdPlaybackTracker(nextAdBreak, playingAdBreak)

        adPlaybackEventEmitter = DefaultAdPlaybackEventEmitter(
            adPlaybackTracker = testAdPlaybackTracker,
            eventEmitter = testEventEmitter,
        )
        testEventEmitter.emittedEvents.clear()
    }

    describe("when next ad break changes") {
        it("emits the next ad break") {
            val adBreak = MediaTailorAdBreak(
                id = "adBreak1",
                ads = emptyList(),
                scheduleTime = 0.0,
                duration = 10.0,
                formattedDuration = "10",
                adMarkerDuration = "10",
            )
            nextAdBreak.emit(adBreak)

            expectThat(testEventEmitter.emittedEvents)
                .isNotEmpty()
                .single()
                .isA<UpcomingAdBreakUpdated>()
                .and {
                    get { adBreak }.isEqualTo(adBreak)
                }
        }
    }

    describe("when playing ad break changes") {
        beforeEach {
            playingAdBreak.emit(
                PlayingAdBreak(
                    adBreak = MediaTailorAdBreak(
                        id = "adBreak1",
                        ads = listOf(
                            MediaTailorLinearAd(
                                id = "ad1",
                                scheduleTime = 0.0,
                                duration = 10.0,
                                formattedDuration = "10",
                                trackingEvents = emptyList(),
                            ),
                        ),
                        scheduleTime = 0.0,
                        duration = 10.0,
                        formattedDuration = "10",
                        adMarkerDuration = "10",
                    ),
                    adIndex = 0,
                ),
            )
        }

        describe("when ad break starts") {
            it("emits ad started event") {
                expectThat(testEventEmitter.emittedEvents).any {
                    isA<MediaTailorEvent.AdStarted>()
                        .and {
                            get { ad.id }.isEqualTo("ad1")
                            get { indexInQueue }.isEqualTo(0)
                        }
                }
            }

            it("emits ad break started event") {
                expectThat(testEventEmitter.emittedEvents).any {
                    isA<MediaTailorEvent.AdBreakStarted>()
                        .and {
                            get { adBreak.id }.isEqualTo("adBreak1")
                        }
                }
            }

            it("ad break started event is emitted before ad started event") {
                expectThat(testEventEmitter.emittedEvents.map { it::class })
                    .containsSequence(
                        MediaTailorEvent.AdBreakStarted::class,
                        MediaTailorEvent.AdStarted::class,
                    )
            }
        }

        describe("when ad break finishes") {
            beforeEach {
                playingAdBreak.emit(null)
            }

            it("emits ad finished event") {
                expectThat(testEventEmitter.emittedEvents).any {
                    isA<MediaTailorEvent.AdFinished>().and {
                        get { ad.id }.isEqualTo("ad1")
                    }
                }
            }

            it("emits ad break finished event") {
                expectThat(testEventEmitter.emittedEvents).any {
                    isA<MediaTailorEvent.AdBreakFinished>().and {
                        get { adBreak.id }.isEqualTo("adBreak1")
                    }
                }
            }

            it("ad finished event is emitted before ad break finished event") {
                expectThat(testEventEmitter.emittedEvents.map { it::class })
                    .containsSequence(
                        MediaTailorEvent.AdFinished::class,
                        MediaTailorEvent.AdBreakFinished::class,
                    )
            }
        }

        describe("when the ad break changes") {
            beforeEach {
                playingAdBreak.emit(
                    PlayingAdBreak(
                        adBreak = MediaTailorAdBreak(
                            id = "adBreak2",
                            ads = listOf(
                                MediaTailorLinearAd(
                                    id = "ad2",
                                    scheduleTime = 0.0,
                                    duration = 10.0,
                                    formattedDuration = "10",
                                    trackingEvents = emptyList(),
                                ),
                            ),
                            scheduleTime = 0.0,
                            duration = 10.0,
                            formattedDuration = "10",
                            adMarkerDuration = "10",
                        ),
                        adIndex = 0,
                    ),
                )
            }

            it("emits ad break finished event for the previous ad break") {
                expectThat(testEventEmitter.emittedEvents).any {
                    isA<MediaTailorEvent.AdBreakFinished>()
                        .and {
                            get { adBreak.id }.isEqualTo("adBreak1")
                        }
                }
            }

            it("emits ad finished event for the previous ad") {
                expectThat(testEventEmitter.emittedEvents).any {
                    isA<MediaTailorEvent.AdFinished>()
                        .and {
                            get { ad.id }.isEqualTo("ad1")
                        }
                }
            }

            it("emits ad break started event for the new ad break") {
                expectThat(testEventEmitter.emittedEvents).any {
                    isA<MediaTailorEvent.AdBreakStarted>()
                        .and {
                            get { adBreak.id }.isEqualTo("adBreak2")
                        }
                }
            }

            it("emits ad started event for the new ad") {
                expectThat(testEventEmitter.emittedEvents).any {
                    isA<MediaTailorEvent.AdStarted>()
                        .and {
                            get { ad.id }.isEqualTo("ad2")
                            get { indexInQueue }.isEqualTo(0)
                        }
                }
            }
        }

        describe("when the current ad changes") {
            beforeEach {
                playingAdBreak.emit(
                    PlayingAdBreak(
                        adBreak = MediaTailorAdBreak(
                            id = "adBreak1",
                            ads = listOf(
                                MediaTailorLinearAd(
                                    id = "ad2",
                                    scheduleTime = 0.0,
                                    duration = 10.0,
                                    formattedDuration = "10",
                                    trackingEvents = emptyList(),
                                ),
                            ),
                            scheduleTime = 0.0,
                            duration = 10.0,
                            formattedDuration = "10",
                            adMarkerDuration = "10",
                        ),
                        adIndex = 0,
                    ),
                )
            }

            it("emits ad finished event") {
                expectThat(testEventEmitter.emittedEvents).any {
                    isA<MediaTailorEvent.AdFinished>()
                        .and {
                            get { ad.id }.isEqualTo("ad1")
                        }
                }
            }

            it("emits ad started event") {
                expectThat(testEventEmitter.emittedEvents).any {
                    isA<MediaTailorEvent.AdStarted>()
                        .and {
                            get { ad.id }.isEqualTo("ad2")
                            get { indexInQueue }.isEqualTo(0)
                        }
                }
            }

            it("ad finished event is emitted before ad started event") {
                expectThat(testEventEmitter.emittedEvents.map { it::class })
                    .containsSequence(
                        MediaTailorEvent.AdFinished::class,
                        MediaTailorEvent.AdStarted::class,
                    )
            }
        }
    }
})
