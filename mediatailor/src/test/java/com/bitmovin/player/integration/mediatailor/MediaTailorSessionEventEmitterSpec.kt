package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import strikt.api.expectThat
import strikt.assertions.containsSequence
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.single
import util.TestEventEmitter
import util.TestMediaTailorSession
import util.UnitSpec

@OptIn(ExperimentalCoroutinesApi::class)
class MediaTailorSessionEventEmitterSpec : UnitSpec({
    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var testEventEmitter: TestEventEmitter
    lateinit var adBreaksFlow: MutableStateFlow<List<MediaTailorAdBreak>>
    lateinit var mediaTailorSession: MediaTailorSession
    lateinit var mediaTailorSessionEventEmitter: MediaTailorSessionEventEmitter

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        Dispatchers.resetMain()
    }

    fun setUp(
        adBreaks: List<MediaTailorAdBreak> = listOf()
    ) {
        adBreaksFlow = MutableStateFlow(adBreaks)
        testEventEmitter = TestEventEmitter()
        mediaTailorSession = TestMediaTailorSession(
            isInitialized = true,
            adBreaks = adBreaksFlow
        )
        mediaTailorSessionEventEmitter = DefaultMediaTailorSessionEventEmitter(
            mediaTailorSession = mediaTailorSession,
            eventEmitter = testEventEmitter,
        )
        testEventEmitter.emittedEvents.clear()
    }

    beforeEach {
        setUp()
    }

    describe("when media tailor session ad breaks are updated with new values") {
        describe("and ad breaks are empty") {
            beforeEach {
                setUp(
                    adBreaks = listOf(
                        MediaTailorAdBreak(
                            id = "adBreak1",
                            ads = emptyList(),
                            scheduleTime = 0.0,
                            duration = 10.0
                        )
                    )
                )
            }
            it("emits an empty ad break schedule updated event") {
                adBreaksFlow.update { listOf() }

                expectThat(testEventEmitter.emittedEvents)
                    .single()
                    .isA<MediaTailorEvent.AdBreakScheduleUpdated>()
                    .and {
                        get { adBreaks }.isEmpty()
                    }
            }
        }

        describe("and ad breaks are not empty") {
            it("emits the ad break schedule updated event with the current ad breaks") {
                val adBreak1 = MediaTailorAdBreak(
                    id = "adBreak1",
                    ads = emptyList(),
                    scheduleTime = 0.0,
                    duration = 10.0
                )
                val adBreak2 = MediaTailorAdBreak(
                    id = "adBreak2",
                    ads = emptyList(),
                    scheduleTime = 10.0,
                    duration = 15.0
                )
                adBreaksFlow.update { (listOf(adBreak1, adBreak2)) }

                expectThat(testEventEmitter.emittedEvents)
                    .single()
                    .isA<MediaTailorEvent.AdBreakScheduleUpdated>()
                    .and {
                        get { adBreaks }.containsSequence(adBreak1, adBreak2)
                    }
            }
        }
    }
})
