package com.bitmovin.player.integration.mediatailor

import android.util.Log
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAssetType
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.model.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.model.MediaTailorSessionInitializationResponse
import com.bitmovin.player.integration.mediatailor.model.MediaTailorTrackingResponse
import com.bitmovin.player.integration.mediatailor.network.HttpClient
import com.bitmovin.player.integration.mediatailor.network.isSuccess
import com.bitmovin.player.integration.mediatailor.util.Disposable
import com.bitmovin.player.integration.mediatailor.util.eventFlow
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

private const val TAG = "MediaTailorSession"

interface MediaTailorSession : Disposable {
    suspend fun initialize(sessionConfig: MediaTailorSessionConfig): Result<String>
    suspend fun fetchTrackingData(): Boolean
    val isInitialized: Boolean
    val adBreaks: StateFlow<List<MediaTailorAdBreak>>
}

internal class DefaultMediaTailorSession(
    private val player: Player,
    private val httpClient: HttpClient,
    private val adsMapper: MediaTailorAdsMapper,
    private val config: MediaTailorSessionConfig,
) : MediaTailorSession {
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val _trackingUrl = MutableStateFlow<String?>(null)
    private val _trackingResponse = MutableStateFlow<MediaTailorTrackingResponse?>(null)
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val _adBreaks = MutableStateFlow<List<MediaTailorAdBreak>>(emptyList())
    override val adBreaks: StateFlow<List<MediaTailorAdBreak>>
        get() = _adBreaks
    private var refreshTrackingResponseJob: Job? = null

    init {
        mainScope.launch {
            _trackingUrl.collect { trackingUrl ->
                trackingUrl?.let {
                    Log.d(TAG, "Tracking URL: $it")
                }
            }
        }
        mainScope.launch {
            _trackingResponse.collect { response ->
                val response = response ?: return@collect
                Log.d(TAG, "Tracking Response: $response")
                _adBreaks.update { adsMapper.mapAdBreaks(response.avails) }
            }
        }
        mainScope.launch {
            player.eventFlow<SourceEvent.Loaded>().collect { event ->
                mainScope.launch { fetchTrackingData() }
                when (val assetType = config.assetType) {
                    MediaTailorAssetType.Vod -> mainScope.launch { fetchTrackingData() }
                    is MediaTailorAssetType.Linear -> {
                        refreshTrackingResponseJob?.cancel()
                        refreshTrackingResponseJob = continuouslyFetchTrackingDataJob(
                            assetType.trackingRequestPollFrequency
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
        pollFrequencySeconds: Double
    ) = mainScope.launch {
        var initialFetchNeeded = true
        while (isActive) {
            if (initialFetchNeeded || player.isPlaying) {
                initialFetchNeeded = false
                fetchTrackingData()
            }
            delay((pollFrequencySeconds * 1000L).toLong())
        }
    }

    override suspend fun initialize(
        sessionConfig: MediaTailorSessionConfig,
    ): Result<String> {
        val response = runCatching {
            requestSessionInitialization(sessionConfig)
        }
        if (response.isFailure) {
            return Result.failure(response.exceptionOrNull()!!)
        }

        val successResponse = response.getOrThrow()
        val baseUri = URI(sessionConfig.sessionInitUrl)
        val manifestUrl = baseUri.resolve(successResponse.manifestUrl).toString()
        val trackingUrl = baseUri.resolve(successResponse.trackingUrl).toString()

        _trackingUrl.update { trackingUrl }

        return Result.success(manifestUrl)
    }

    override suspend fun fetchTrackingData(): Boolean {
        return runCatching {
            requestTrackingData()
        }.isSuccess
    }

    override val isInitialized: Boolean
        get() = _trackingUrl.value != null

    private suspend fun requestTrackingData() {
        val trackingUrl = _trackingUrl.value ?: return
        val response = requestTrackingData(trackingUrl)
        _trackingResponse.update { response }
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
        mainScope.cancel()
    }
}
