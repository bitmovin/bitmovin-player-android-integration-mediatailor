package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.integration.mediatailor.api.LinearAssetTypeConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAssetType
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.network.HttpClient
import com.bitmovin.player.integration.mediatailor.network.HttpRequestResult
import com.bitmovin.player.integration.mediatailor.util.eventFlow
import io.kotest.core.spec.style.DescribeSpec
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
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import util.mockkPlayerExtension
import util.unmockkPlayerExtension

@OptIn(ExperimentalCoroutinesApi::class)
class MediaTailorSessionPaginationSpec : DescribeSpec({
    val testDispatcher = UnconfinedTestDispatcher()
    val pollFrequency = 4.seconds

    class FakeHttpClient : HttpClient {
        val getRequests = mutableListOf<String>()
        val responses = ArrayDeque<HttpRequestResult>()

        override suspend fun get(url: String): HttpRequestResult {
            getRequests.add(url)
            return responses.removeFirst()
        }

        override suspend fun post(url: String, body: Map<String, Any>): HttpRequestResult = HttpRequestResult.Success(body = """{"manifestUrl": "$MANIFEST_URL", "trackingUrl": "$TRACKING_URL"}""")
    }

    lateinit var httpClient: FakeHttpClient
    lateinit var session: DefaultMediaTailorSession
    lateinit var loadedFlow: MutableSharedFlow<SourceEvent.Loaded>
    lateinit var unloadedFlow: MutableSharedFlow<SourceEvent.Unloaded>

    suspend fun setUp(config: LinearAssetTypeConfig) {
        httpClient = FakeHttpClient()
        loadedFlow = MutableSharedFlow()
        unloadedFlow = MutableSharedFlow()
        val player = mockk<Player>(relaxed = true)
        every { player.eventFlow<SourceEvent.Loaded>() } returns loadedFlow
        every { player.eventFlow<SourceEvent.Unloaded>() } returns unloadedFlow
        every { player.isPlaying } returns true
        session = DefaultMediaTailorSession(player, httpClient, DefaultAdsMapper())
        session.initialize(
            MediaTailorSessionConfig(
                sessionInitUrl = "https://example.com/session-init",
                assetType = MediaTailorAssetType.Linear(config),
            ),
        )
    }

    suspend fun load() = loadedFlow.emit(SourceEvent.Loaded(source = mockk()))

    fun poll() = testDispatcher.scheduler.advanceTimeBy(pollFrequency.inWholeMilliseconds + 1)

    fun ok(avails: String = "", nextToken: String? = null) = HttpRequestResult.Success(
        body = """{"avails": [$avails], "nextToken": ${nextToken?.let { "\"$it\"" }}}""",
    )

    fun adIds() = session.adBreaks.value.map { breakAds -> breakAds.ads.map { it.id } }

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
        mockkPlayerExtension()
    }

    afterSpec {
        Dispatchers.resetMain()
        unmockkPlayerExtension()
    }

    afterEach {
        session.dispose()
    }

    describe("a linear session with pagination enabled") {
        beforeEach {
            setUp(LinearAssetTypeConfig(trackingRequestPollFrequency = pollFrequency, trackingRequestPaginationEnabled = true))
        }

        it("sends the first request without a token") {
            httpClient.responses.add(ok(nextToken = "abc%3D"))

            load()

            expectThat(httpClient.getRequests).containsExactly(TRACKING_URL)
        }

        it("sends the token of the previous response with the next request") {
            httpClient.responses.add(ok(nextToken = "abc%3D"))
            httpClient.responses.add(ok(nextToken = "abc=="))
            httpClient.responses.add(ok(nextToken = "def=="))

            load()
            poll()
            poll()

            expectThat(httpClient.getRequests).containsExactly(
                TRACKING_URL,
                "$TRACKING_URL?aws.nextToken=abc%3D",
                "$TRACKING_URL?aws.nextToken=abc%3D%3D",
            )
        }

        it("replaces the ad breaks of a response without token and merges those of a response with token") {
            httpClient.responses.add(ok(avail("1", 10.0, "a1", "a2"), nextToken = "t1"))
            httpClient.responses.add(ok(avail("1", 10.0, "a3", startOffset = 30.0) + "," + avail("2", 50.0, "b1"), nextToken = "t2"))

            load()
            expectThat(adIds()).isEqualTo(listOf(listOf("a1", "a2")))

            poll()
            expectThat(adIds()).isEqualTo(listOf(listOf("a1", "a2", "a3"), listOf("b1")))
        }

        it("keeps the ad breaks when a response with token has no avails") {
            httpClient.responses.add(ok(avail("1", 10.0, "a1"), nextToken = "t1"))
            httpClient.responses.add(ok(nextToken = "t1"))

            load()
            poll()

            expectThat(adIds()).isEqualTo(listOf(listOf("a1")))
        }

        it("falls back to a full request after a failed request with token") {
            httpClient.responses.add(ok(avail("1", 10.0, "a1"), nextToken = "t1"))
            httpClient.responses.add(HttpRequestResult.Failure(body = """{"message": "bad token"}"""))
            httpClient.responses.add(ok(avail("1", 10.0, "a1", "a2"), nextToken = "t2"))
            httpClient.responses.add(ok(nextToken = "t2"))

            load()
            poll()
            expectThat(adIds()).isEqualTo(listOf(listOf("a1")))
            poll()
            poll()

            expectThat(httpClient.getRequests).containsExactly(
                TRACKING_URL,
                "$TRACKING_URL?aws.nextToken=t1",
                TRACKING_URL,
                "$TRACKING_URL?aws.nextToken=t2",
            )
            expectThat(adIds()).isEqualTo(listOf(listOf("a1", "a2")))
        }

        it("falls back to a full request when a response with token cannot be parsed") {
            httpClient.responses.add(ok(nextToken = "t1"))
            httpClient.responses.add(HttpRequestResult.Success(body = "not json"))
            httpClient.responses.add(ok(nextToken = "t2"))

            load()
            poll()
            poll()

            expectThat(httpClient.getRequests).containsExactly(
                TRACKING_URL,
                "$TRACKING_URL?aws.nextToken=t1",
                TRACKING_URL,
            )
        }

        it("keeps the token when a request without token fails") {
            httpClient.responses.add(HttpRequestResult.Failure())
            httpClient.responses.add(ok(nextToken = "t1"))
            httpClient.responses.add(ok(nextToken = "t1"))

            load()
            poll()
            poll()

            expectThat(httpClient.getRequests).containsExactly(
                TRACKING_URL,
                TRACKING_URL,
                "$TRACKING_URL?aws.nextToken=t1",
            )
        }

        it("sends a full request after a response with a null token") {
            httpClient.responses.add(ok(avail("1", 10.0, "a1"), nextToken = "t1"))
            httpClient.responses.add(ok(avail("2", 50.0, "b1"), nextToken = null))
            httpClient.responses.add(ok(avail("2", 50.0, "b1"), nextToken = "t2"))

            load()
            poll()
            poll()

            expectThat(httpClient.getRequests).containsExactly(
                TRACKING_URL,
                "$TRACKING_URL?aws.nextToken=t1",
                TRACKING_URL,
            )
            expectThat(adIds()).isEqualTo(listOf(listOf("b1")))
        }

        it("sends a full request after the source was unloaded") {
            httpClient.responses.add(ok(nextToken = "t1"))
            httpClient.responses.add(ok(nextToken = "t2"))

            load()
            unloadedFlow.emit(SourceEvent.Unloaded())
            load()

            expectThat(httpClient.getRequests).containsExactly(TRACKING_URL, TRACKING_URL)
        }
    }

    describe("a linear session with pagination disabled") {
        beforeEach {
            setUp(LinearAssetTypeConfig(trackingRequestPollFrequency = pollFrequency))
        }

        it("never sends a token and replaces the ad breaks on every response") {
            httpClient.responses.add(ok(avail("1", 10.0, "a1", "a2"), nextToken = "t1"))
            httpClient.responses.add(ok(avail("2", 50.0, "b1"), nextToken = "t2"))
            httpClient.responses.add(HttpRequestResult.Failure())

            load()
            poll()
            expectThat(adIds()).isEqualTo(listOf(listOf("b1")))
            poll()

            expectThat(httpClient.getRequests).containsExactly(TRACKING_URL, TRACKING_URL, TRACKING_URL)
            expectThat(adIds()).isEqualTo(listOf(listOf("b1")))
        }

        it("emits no ad breaks before a response") {
            expectThat(session.adBreaks.value).isEmpty()
        }
    }
})

private const val TRACKING_URL = "https://example.com/tracking"
private const val MANIFEST_URL = "https://example.com/manifest.m3u8"

private fun avail(availId: String, start: Double, vararg adIds: String, startOffset: Double = 0.0): String {
    val ads = adIds.mapIndexed { index, adId ->
        val adStart = start + startOffset + index * 15
        """{"adId": "$adId", "duration": "PT15S", "durationInSeconds": 15.0, "startTime": "PT${adStart}S",
            "startTimeInSeconds": $adStart, "trackingEvents": []}"""
    }
    return """{"availId": "$availId", "duration": "PT30S", "durationInSeconds": 30.0, "startTime": "PT${start}S",
        "startTimeInSeconds": $start, "adMarkerDuration": "PT30S", "ads": [${ads.joinToString(",")}]}"""
}
