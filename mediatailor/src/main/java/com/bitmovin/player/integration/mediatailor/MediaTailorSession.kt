package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAssetType
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.model.MediaTailorSessionInitializationResponse
import com.bitmovin.player.integration.mediatailor.model.MediaTailorTrackingResponse
import com.bitmovin.player.integration.mediatailor.network.HttpClient
import com.bitmovin.player.integration.mediatailor.network.isSuccess
import com.bitmovin.player.integration.mediatailor.util.Disposable
import com.bitmovin.player.integration.mediatailor.util.eventFlow
import com.bitmovin.player.integration.mediatailor.util.runCatchingCooperative
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okio.IOException
import java.net.URI
import kotlin.time.Duration

private const val TAG = "MediaTailorSession"

internal interface MediaTailorSession : Disposable {
    suspend fun initialize(sessionConfig: MediaTailorSessionConfig): SessionInitializationResult
    val isInitialized: Boolean
    val adBreaks: StateFlow<List<MediaTailorAdBreak>>
}

internal class DefaultMediaTailorSession(
    private val player: Player,
    private val httpClient: HttpClient,
    private val adsMapper: MediaTailorAdsMapper,
    private val config: MediaTailorSessionConfig,
) : MediaTailorSession {
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val _trackingUrl = MutableStateFlow<String?>(null)
    private val _adBreaks = MutableStateFlow<List<MediaTailorAdBreak>>(emptyList())
    override val adBreaks: StateFlow<List<MediaTailorAdBreak>>
        get() = _adBreaks
    private var refreshTrackingResponseJob: Job? = null

    init {
        mainScope.launch {
            player.eventFlow<SourceEvent.Loaded>().collect { event ->
                when (val assetType = config.assetType) {
                    MediaTailorAssetType.Vod -> mainScope.launch { fetchTrackingData() }
                    is MediaTailorAssetType.Linear -> {
                        refreshTrackingResponseJob?.cancel()
                        refreshTrackingResponseJob = continuouslyFetchTrackingDataJob(
                            assetType.config.trackingRequestPollFrequency
                        )
                    }
                }
            }
        }
        mainScope.launch {
            player.eventFlow<SourceEvent.Unloaded>().collect { event ->
                refreshTrackingResponseJob?.cancel()
                refreshTrackingResponseJob = null
            }
        }
    }

    private fun continuouslyFetchTrackingDataJob(
        pollFrequency: Duration,
    ) = mainScope.launch {
        var initialFetchNeeded = true
        while (isActive) {
            if (initialFetchNeeded || player.isPlaying) {
                initialFetchNeeded = false
                fetchTrackingData()
            }
            delay(pollFrequency.inWholeMilliseconds)
        }
    }

    override suspend fun initialize(
        sessionConfig: MediaTailorSessionConfig,
    ): SessionInitializationResult {
        val response = runCatchingCooperative {
            requestSessionInitialization(sessionConfig)
        }

        if (response.isFailure) {
            return SessionInitializationResult.Failure(response.exceptionOrNull()!!.message)
        }

        val successResponse = response.getOrThrow()
        val baseUri = URI(sessionConfig.sessionInitUrl)
        val manifestUrl = baseUri.resolve(successResponse.manifestUrl).toString()
        val trackingUrl = baseUri.resolve(successResponse.trackingUrl).toString()

        _trackingUrl.update { trackingUrl }

        return SessionInitializationResult.Success(manifestUrl = manifestUrl)
    }

    private suspend fun fetchTrackingData(): Boolean = runCatchingCooperative {
        requestTrackingData()
    }.isSuccess

    override val isInitialized: Boolean
        get() = _trackingUrl.value != null

    private suspend fun requestTrackingData() {
        val trackingUrl = _trackingUrl.value ?: return
        val response = requestTrackingData(trackingUrl)
        _adBreaks.update { adsMapper.mapAdBreaks(response.avails) }
    }

    /**
     * Throws:
     * - [IOException] if the request fails
     * - [kotlinx.serialization.SerializationException] if the response cannot be parsed
     */
    private suspend fun requestSessionInitialization(
        sessionConfig: MediaTailorSessionConfig
    ): MediaTailorSessionInitializationResponse {
        val response = httpClient.post(
            sessionConfig.sessionInitUrl,
            sessionConfig.sessionInitParams
        )
        return if (response.isSuccess) {
            json.decodeFromString<MediaTailorSessionInitializationResponse>(response.body!!)
        } else {
            throw IOException("Failed to initialize MediaTailor session")
        }
    }

    /**
     * Throws:
     * - [IOException] if the request fails
     * - [kotlinx.serialization.SerializationException] if the response cannot be parsed
     */
    private suspend fun requestTrackingData(trackingUrl: String): MediaTailorTrackingResponse {
        val response = httpClient.get(trackingUrl)
        return if (response.isSuccess) {
            json.decodeFromString<MediaTailorTrackingResponse>(response.body!!)
        } else {
            throw IOException("Failed to refresh MediaTailor session")
        }
    }

    override fun dispose() {
        _trackingUrl.update { null }
        _adBreaks.update { emptyList() }
        mainScope.cancel()
    }
}
