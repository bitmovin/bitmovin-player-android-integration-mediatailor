package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.integration.mediatailor.api.MediaTailorAssetType
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionManager
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult.Success
import com.bitmovin.player.integration.mediatailor.api.TrackingEvent
import com.bitmovin.player.integration.mediatailor.eventEmitter.InternalEventEmitter
import com.bitmovin.player.integration.mediatailor.util.DependencyFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import strikt.api.expectThat
import strikt.assertions.isA
import util.UnitSpec
import util.mockkPlayerExtension
import util.unmockkPlayerExtension

@OptIn(ExperimentalCoroutinesApi::class)
class MediaTailorSessionManagerSpec : UnitSpec({
    lateinit var adPlaybackTracker: AdPlaybackTracker
    lateinit var adPlaybackEventEmitter: AdPlaybackEventEmitter
    lateinit var mediaTailorSessionEmitter: MediaTailorSessionEventEmitter
    lateinit var adBeaconing: AdBeaconing
    lateinit var eventEmitter: InternalEventEmitter
    lateinit var mediaTailorSession: MediaTailorSession
    lateinit var mediaTailorSessionManager: MediaTailorSessionManager
    lateinit var dependencyFactory: DependencyFactory

    beforeEach {
        adPlaybackTracker = mockk(relaxed = true)
        adPlaybackEventEmitter = mockk(relaxed = true)
        mediaTailorSessionEmitter = mockk(relaxed = true)
        adBeaconing = mockk(relaxed = true)
        eventEmitter = mockk(relaxed = true)
        mediaTailorSession = mockk(relaxed = true)
        dependencyFactory = mockk<DependencyFactory> {
            every { createHttpClient() } returns mockk(relaxed = true)
            every { createAdsMapper() } returns mockk(relaxed = true)
            every { createEventEmitter() } returns eventEmitter
            every { createMediaTailorSession(any(), any(), any()) } returns mediaTailorSession
            every { createAdPlaybackTracker(any(), any()) } returns adPlaybackTracker
            every { createAdPlaybackEventEmitter(any(), any()) } returns adPlaybackEventEmitter
            every {
                createMediaTailorSessionEventEmitter(
                    any(),
                    any()
                )
            } returns mediaTailorSessionEmitter
            every { createAdBeaconing(any(), any(), any(), any()) } returns adBeaconing
        }
        mediaTailorSessionManager = DefaultMediaTailorSessionManager(
            mockk(relaxed = true),
            dependencyFactory
        )
    }

    beforeSpec {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkPlayerExtension()
    }

    afterSpec {
        Dispatchers.resetMain()
        unmockkPlayerExtension()
    }

    describe("initializing a media tailor session") {
        describe("when initialization is successful") {
            beforeEach {
                coEvery { mediaTailorSession.initialize(any()) } returns Success("")
            }

            it("creates dependencies") {
                mediaTailorSessionManager.initializeSession(
                    sessionConfig = MediaTailorSessionConfig(
                        sessionInitUrl = "https://example.com/session-init",
                        assetType = MediaTailorAssetType.Vod,
                    )
                )

                verify {
                    dependencyFactory.createAdPlaybackTracker(any(), any())
                    dependencyFactory.createAdPlaybackEventEmitter(any(), any())
                    dependencyFactory.createMediaTailorSessionEventEmitter(any(), any())
                    dependencyFactory.createAdBeaconing(any(), any(), any(), any())
                }
            }
        }

        describe("when initialization fails") {
            beforeEach {
                coEvery { mediaTailorSession.initialize(any()) } returns SessionInitializationResult.Failure(
                    "Initialization failed"
                )
            }

            it("does not create dependencies") {
                mediaTailorSessionManager.initializeSession(
                    sessionConfig = MediaTailorSessionConfig(
                        sessionInitUrl = "https://example.com/session-init",
                        assetType = MediaTailorAssetType.Vod,
                    )
                )

                verify(exactly = 0) {
                    dependencyFactory.createAdPlaybackTracker(any(), any())
                    dependencyFactory.createAdPlaybackEventEmitter(any(), any())
                    dependencyFactory.createMediaTailorSessionEventEmitter(any(), any())
                    dependencyFactory.createAdBeaconing(any(), any(), any(), any())
                }
            }

            describe("sending a tracking event") {
                it("emits an error event") {
                    mediaTailorSessionManager.sendTrackingEvent(TrackingEvent.ClickTracking)
                    val slot = slot<MediaTailorEvent>()
                    verify { eventEmitter.emit(capture(slot)) }

                    expectThat(slot.captured).isA<MediaTailorEvent.Error>()
                }
            }
        }

        describe("when session is already initialized") {
            beforeEach {
                coEvery { mediaTailorSession.initialize(any()) } returns Success("")
                mediaTailorSessionManager.initializeSession(
                    sessionConfig = MediaTailorSessionConfig(
                        sessionInitUrl = "https://example.com/session-init",
                        assetType = MediaTailorAssetType.Vod,
                    )
                )
            }

            it("does not allow re-initialization") {
                val result = mediaTailorSessionManager.initializeSession(
                    sessionConfig = MediaTailorSessionConfig(
                        sessionInitUrl = "https://example.com/session-init",
                        assetType = MediaTailorAssetType.Vod,
                    )
                )

                expectThat(result).isA<SessionInitializationResult.Failure>()
            }

            describe("sending a tracking event") {
                beforeEach {
                    coEvery { mediaTailorSession.initialize(any()) } returns Success("")
                    mediaTailorSessionManager.initializeSession(
                        sessionConfig = MediaTailorSessionConfig(
                            sessionInitUrl = "https://example.com/session-init",
                            assetType = MediaTailorAssetType.Vod,
                        )
                    )
                }

                it("tracks the event using ad beaconing") {
                    mediaTailorSessionManager.sendTrackingEvent(TrackingEvent.ClickTracking)

                    verify { adBeaconing.track(TrackingEvent.ClickTracking.eventType) }
                }

                it("tracks other event using ad beaconing") {
                    mediaTailorSessionManager.sendTrackingEvent(TrackingEvent.Other("otherEvent"))

                    verify { adBeaconing.track("otherEvent") }
                }
            }
        }
    }

    describe("before media tailor session is initialized") {
        it("does not allow sending tracking events") {
            mediaTailorSessionManager.sendTrackingEvent(TrackingEvent.ClickTracking)

            val slot = slot<MediaTailorEvent>()
            verify { eventEmitter.emit(capture(slot)) }

            expectThat(slot.captured).isA<MediaTailorEvent.Error>()
        }
    }

    describe("stopping a media tailor session") {
        beforeEach {
            coEvery { mediaTailorSession.initialize(any()) } returns Success("")
            mediaTailorSessionManager.initializeSession(
                sessionConfig = MediaTailorSessionConfig(
                    sessionInitUrl = "https://example.com/session-init",
                    assetType = MediaTailorAssetType.Vod,
                )
            )
        }

        it("disposes the dependencies") {
            mediaTailorSessionManager.stopSession()

            verify {
                mediaTailorSession.dispose()
                adPlaybackTracker.dispose()
                adPlaybackEventEmitter.dispose()
                mediaTailorSessionEmitter.dispose()
                adBeaconing.dispose()
            }
        }
    }
})
