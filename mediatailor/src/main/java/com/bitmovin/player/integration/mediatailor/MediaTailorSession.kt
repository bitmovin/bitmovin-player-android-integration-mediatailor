package com.bitmovin.player.integration.mediatailor

import android.util.Log
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.core.internal.InternalEventEmitter
import com.bitmovin.player.integration.mediatailor.model.MediaTailorSessionInitializationResponse
import com.bitmovin.player.integration.mediatailor.model.MediaTailorTrackingResponse
import com.bitmovin.player.integration.mediatailor.network.HttpClient
import com.bitmovin.player.integration.mediatailor.network.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okio.IOException
import java.net.URI

private const val TAG = "MediaTailorSession"

interface MediaTailorSession : Disposable {
    suspend fun initialize(mediaTailorSourceConfig: MediaTailorSourceConfig): Result<SourceConfig>
    suspend fun fetchTrackingData(): Boolean
    val isInitialized: Boolean
    val adBreaks: StateFlow<List<MediaTailorAdBreak>>
}

internal class DefaultMediaTailorSession(
    private val httpClient: HttpClient,
    private val eventEmitter: InternalEventEmitter<Event>,
    private val adsMapper: MediaTailorAdsMapper,
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

    private val seenAdBreakIds = mutableSetOf<String>()
    private val seenAdIds = mutableSetOf<String>()

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
                _adBreaks.value = adsMapper.mapAdBreaks(response.avails)

                var newAdsScheduledCount = 0
                _adBreaks.value.forEach { adBreak ->
                    if (adBreak.id !in seenAdBreakIds) {
                        seenAdBreakIds.add(adBreak.id)
                        adBreak.ads.forEach { ad ->
                            if (ad.id !in seenAdIds) {
                                seenAdIds.add(ad.id!!)
                                newAdsScheduledCount++
                            }
                        }
                    }
                }
                if (newAdsScheduledCount > 0) {
                    eventEmitter.emit(PlayerEvent.AdScheduled(newAdsScheduledCount))
                }
            }
        }
    }

    override suspend fun initialize(
        mediaTailorSourceConfig: MediaTailorSourceConfig
    ): Result<SourceConfig> {
        val sessionConfig = mediaTailorSourceConfig.sessionConfig
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

        return Result.success(mediaTailorSourceConfig.toSourceConfig(manifestUrl))
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
        _trackingResponse.value = response
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
