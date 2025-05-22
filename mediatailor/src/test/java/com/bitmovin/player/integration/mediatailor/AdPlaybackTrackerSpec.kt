@file:OptIn(ExperimentalCoroutinesApi::class)

package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult
import com.bitmovin.player.integration.mediatailor.util.eventFlow
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import util.UnitSpec

class AdPlaybackTrackerSpec : UnitSpec({
    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var player: Player
    lateinit var adBreaksFlow: MutableStateFlow<List<MediaTailorAdBreak>>
    lateinit var adPlaybackTracker: AdPlaybackTracker
    val timeChangedFlow = MutableSharedFlow<PlayerEvent.TimeChanged>()
    val timeShiftedFlow = MutableSharedFlow<PlayerEvent.TimeShifted>()
    val seekedFlow = MutableSharedFlow<PlayerEvent.Seeked>()

    fun createAdPlaybackTracker(
        adBreaks: List<MediaTailorAdBreak> = emptyList()
    ) {
        adBreaksFlow = MutableStateFlow(adBreaks)
        val fakeMediaTailorSession = FakeMediaTailorSession(
            isInitialized = true,
            adBreaks = adBreaksFlow,
        )

        adPlaybackTracker = DefaultAdPlaybackTracker(
            player,
            fakeMediaTailorSession,
        )
    }

    beforeSpec {
        mockkStatic("com.bitmovin.player.integration.mediatailor.util.PlayerExtension")
    }

    afterSpec {
        unmockkStatic("com.bitmovin.player.integration.mediatailor.util.PlayerExtension")
    }

    beforeEach {
        Dispatchers.setMain(testDispatcher)

        player = mockk<Player>(relaxed = true) {
            every { currentTime } returns -1.0
        }

        createAdPlaybackTracker()
    }

    describe("initializing the ad playback tracker") {
        describe("with no ad breaks in the session") {
            beforeEach {
                createAdPlaybackTracker()
            }

            it("currentAdBreak is null") {
                expectThat(adPlaybackTracker.currentAdBreak.value).isNull()
            }

            it("nextAdBreak is null") {
                expectThat(adPlaybackTracker.nextAdBreak.value).isNull()
            }

            it("currentAd is null") {
                expectThat(adPlaybackTracker.currentAd.value).isNull()
            }
        }

        describe("with an ad break in the present") {
            lateinit var adBreak: MediaTailorAdBreak
            beforeEach {
                player = mockk<Player>(relaxed = true) {
                    every { currentTime } returns 5.0
                }
                adBreak = MediaTailorAdBreak(
                    id = "adBreak1",
                    ads = listOf(),
                    scheduleTime = 4.0,
                    duration = 5.0,
                )
                createAdPlaybackTracker(listOf(adBreak))
            }

            it("currentAdBreak is the correct adBreak") {
                expectThat(adPlaybackTracker.currentAdBreak.value).isEqualTo(adBreak)
            }
        }

        describe("with an ad break in the past") {
            lateinit var adBreak: MediaTailorAdBreak
            beforeEach {
                player = mockk<Player>(relaxed = true) {
                    every { currentTime } returns 5.0
                }
                adBreak = MediaTailorAdBreak(
                    id = "adBreak1",
                    ads = listOf(),
                    scheduleTime = 0.0,
                    duration = 4.0,
                )
                createAdPlaybackTracker(listOf(adBreak))
            }

            it("currentAdBreak is null") {
                expectThat(adPlaybackTracker.currentAdBreak.value).isNull()
            }
        }

        describe("with an ad break in the future") {
            lateinit var adBreak: MediaTailorAdBreak
            beforeEach {
                player = mockk<Player>(relaxed = true) {
                    every { currentTime } returns 5.0
                }
                adBreak = MediaTailorAdBreak(
                    id = "adBreak1",
                    ads = listOf(),
                    scheduleTime = 10.0,
                    duration = 5.0,
                )
                createAdPlaybackTracker(listOf(adBreak))
            }

            it("currentAdBreak is null") {
                expectThat(adPlaybackTracker.currentAdBreak.value).isNull()
            }

            it("nextAdBreak is the correct adBreak") {
                expectThat(adPlaybackTracker.nextAdBreak.value).isEqualTo(adBreak)
            }
        }

        describe("with ad break in the future and current ad break") {
            lateinit var currentAdBreak: MediaTailorAdBreak
            lateinit var nextAdBreak: MediaTailorAdBreak
            beforeEach {
                player = mockk<Player>(relaxed = true) {
                    every { currentTime } returns 5.0
                }
                currentAdBreak = MediaTailorAdBreak(
                    id = "adBreak1",
                    ads = listOf(),
                    scheduleTime = 4.0,
                    duration = 3.0,
                )
                nextAdBreak = MediaTailorAdBreak(
                    id = "adBreak2",
                    ads = listOf(),
                    scheduleTime = 10.0,
                    duration = 5.0,
                )
                createAdPlaybackTracker(listOf(currentAdBreak, nextAdBreak))
            }

            // TODO: Fix this case
            xit("currentAdBreak is the correct adBreak") {
                expectThat(adPlaybackTracker.currentAdBreak.value).isEqualTo(currentAdBreak)
            }

            it("nextAdBreak is the correct adBreak") {
                expectThat(adPlaybackTracker.nextAdBreak.value).isEqualTo(nextAdBreak)
            }
        }
    }

    describe("when player receives seeking and time shifting events") {
        lateinit var firstAdBreak: MediaTailorAdBreak
        lateinit var secondAdBreak: MediaTailorAdBreak

        beforeEach {
            player = mockk<Player>(relaxed = true)
            every { player.currentTime } returns 11.0
            every { player.eventFlow<PlayerEvent.TimeShifted>() } returns timeShiftedFlow
            every { player.eventFlow<PlayerEvent.Seeked>() } returns seekedFlow
            firstAdBreak = MediaTailorAdBreak(
                id = "adBreak1",
                ads = listOf(),
                scheduleTime = 4.0,
                duration = 3.0,
            )
            secondAdBreak = MediaTailorAdBreak(
                id = "adBreak2",
                ads = listOf(),
                scheduleTime = 10.0,
                duration = 5.0,
            )
            createAdPlaybackTracker(listOf(firstAdBreak, secondAdBreak))

            expectThat(adPlaybackTracker.currentAdBreak.value).isEqualTo(secondAdBreak)
            expectThat(adPlaybackTracker.nextAdBreak.value).isNull()
        }

        describe("after time shift") {
            beforeEach {
                every { player.currentTime } returns 9.0
            }

            it("resets next and current ad breaks") {
                timeShiftedFlow.emit(PlayerEvent.TimeShifted())

                expectThat(adPlaybackTracker.currentAdBreak.value).isEqualTo(null)
                expectThat(adPlaybackTracker.nextAdBreak.value).isEqualTo(secondAdBreak)
            }
        }

        describe("after seeked") {
            beforeEach {
                every { player.currentTime } returns 9.0
            }

            it("resets next and current ad breaks") {
                seekedFlow.emit(PlayerEvent.Seeked())

                expectThat(adPlaybackTracker.currentAdBreak.value).isEqualTo(null)
                expectThat(adPlaybackTracker.nextAdBreak.value).isEqualTo(secondAdBreak)
            }
        }
    }

    describe("when the player time changes") {
        lateinit var firstAdBreak: MediaTailorAdBreak
        lateinit var secondAdBreak: MediaTailorAdBreak
        lateinit var thirdAdBreak: MediaTailorAdBreak
        var currentTime = 0.0

        beforeEach {
            player = mockk<Player>(relaxed = true)
            every { player.currentTime } returns currentTime
            every { player.eventFlow<PlayerEvent.TimeChanged>() } returns timeChangedFlow
            firstAdBreak = MediaTailorAdBreak(
                id = "adBreak1",
                ads = listOf(),
                scheduleTime = 5.0,
                duration = 3.0,
            )
            secondAdBreak = MediaTailorAdBreak(
                id = "adBreak2",
                ads = listOf(),
                scheduleTime = 10.0,
                duration = 3.0,
            )
            thirdAdBreak = MediaTailorAdBreak(
                id = "adBreak3",
                ads = listOf(),
                scheduleTime = 15.0,
                duration = 3.0,
            )
            createAdPlaybackTracker(listOf(firstAdBreak, secondAdBreak))
        }

        describe("when time changes to 5.0") {
            beforeEach {
                currentTime = 5.0
                every { player.currentTime } returns currentTime
            }

            it("current ad break is the first ad break") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(currentTime))

                expectThat(adPlaybackTracker.currentAdBreak.value).isEqualTo(firstAdBreak)
            }

            // TODO: Fix this case
            xit("next ad break is the second ad break") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(currentTime))

                expectThat(adPlaybackTracker.nextAdBreak.value).isEqualTo(secondAdBreak)
            }
        }

        describe("when time changes to 12.0") {
            beforeEach {
                currentTime = 12.0
                every { player.currentTime } returns currentTime
            }

            it("current ad break is the second ad break") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(currentTime))

                expectThat(adPlaybackTracker.currentAdBreak.value).isEqualTo(secondAdBreak)
            }

            // TODO: Fix this case
            xit("next ad break is the third ad break") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(currentTime))

                expectThat(adPlaybackTracker.nextAdBreak.value).isEqualTo(thirdAdBreak)
            }
        }

        describe("when time changes to 20.0") {
            beforeEach {
                currentTime = 20.0
                every { player.currentTime } returns currentTime
            }

            it("current ad break is the third ad break") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(currentTime))

                expectThat(adPlaybackTracker.currentAdBreak.value).isEqualTo(thirdAdBreak)
            }

            it("next ad break is null") {
                timeChangedFlow.emit(PlayerEvent.TimeChanged(currentTime))

                expectThat(adPlaybackTracker.nextAdBreak.value).isNull()
            }
        }
    }
})

class FakeMediaTailorSession(
    override val isInitialized: Boolean,
    override val adBreaks: StateFlow<List<MediaTailorAdBreak>>
) : MediaTailorSession {
    override suspend fun initialize(sessionConfig: MediaTailorSessionConfig): SessionInitializationResult {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }
}
