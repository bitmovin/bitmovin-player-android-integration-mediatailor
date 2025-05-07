package com.bitmovin.player.integration.mediatailor

import android.util.Log
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.core.internal.InternalEventEmitter
import com.bitmovin.player.integration.mediatailor.model.ImplicitSessionStartResponse
import com.bitmovin.player.integration.mediatailor.model.MediaTailorTrackingResponse
import com.bitmovin.player.integration.mediatailor.network.HttpClient
import com.bitmovin.player.integration.mediatailor.network.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URI

private const val TAG = "MediaTailorSession"

internal interface MediaTailorSession : Disposable {
    suspend fun initialize(mediaTailorSourceConfig: MediaTailorSourceConfig): Result<SourceConfig>
    val adBreaks: List<MediaTailorAdBreak>
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
    private var refreshTrackingResponseJob: Job? = null
    private val _adBreaks = MutableStateFlow<List<MediaTailorAdBreak>>(emptyList())
    override val adBreaks: List<MediaTailorAdBreak>
        get() = _adBreaks.value

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
        val result = when (val sessionConfig = mediaTailorSourceConfig.mediaTailorSessionConfig) {
            is MediaTailorSessionConfig.Implicit -> {
                val response = prepareImplicitSession(sessionConfig)
                val baseUri = URI(sessionConfig.sessionInitUrl)
                val manifestUrl = baseUri.resolve(response.manifestUrl).toString()
                _trackingUrl.value = baseUri.resolve(response.trackingUrl).toString()
                _trackingResponse.value = initializeSession(
                    MediaTailorSessionConfig.Explicit(
                        manifestUrl = manifestUrl,
                        trackingUrl = _trackingUrl.value!!,
                        assetType = sessionConfig.assetType
                    )
                )

                Result.success(mediaTailorSourceConfig.toSourceConfig(manifestUrl))
            }

            is MediaTailorSessionConfig.Explicit -> {
                _trackingResponse.value = initializeSession(sessionConfig)
                _trackingUrl.value = sessionConfig.trackingUrl

                Result.success(
                    mediaTailorSourceConfig.toSourceConfig(
                        sessionConfig.manifestUrl
                    )
                )
            }
        }

        if (mediaTailorSourceConfig.mediaTailorSessionConfig.assetType == MediaTailorAssetType.Linear) {
            refreshTrackingResponseJob = mainScope.launch {
                while (isActive) {
                    refreshTrackingResponse()
                    // Web integration makes a request for every segment playback event. Android doesn't have that.
                    delay(4_000)
                }
            }
        }

        return result
    }

    private suspend fun refreshTrackingResponse() {
        val trackingUrl = _trackingUrl.value ?: return
        val response = refreshTrackingResponse(trackingUrl)
        _trackingResponse.value = response
    }

    private suspend fun prepareImplicitSession(
        sessionConfig: MediaTailorSessionConfig.Implicit
    ): ImplicitSessionStartResponse {
        val response = httpClient.post(sessionConfig.sessionInitUrl)
        return if (response.isSuccess) {
            json.decodeFromString<ImplicitSessionStartResponse>(response.body!!)
        } else {
            throw IllegalStateException("Failed to initialize MediaTailor session")
        }
    }

    private suspend fun initializeSession(
        sessionConfig: MediaTailorSessionConfig.Explicit
    ): MediaTailorTrackingResponse {
        val response = httpClient.get(sessionConfig.trackingUrl)
        return if (response.isSuccess) {
            json.decodeFromString<MediaTailorTrackingResponse>(response.body!!)
        } else {
            throw IllegalStateException("Failed to initialize MediaTailor session")
        }
    }

    private suspend fun refreshTrackingResponse(trackingUrl: String): MediaTailorTrackingResponse {
        val response = httpClient.get(trackingUrl)
        return if (response.isSuccess) {
            json.decodeFromString<MediaTailorTrackingResponse>(response.body!!)
        } else {
            throw IllegalStateException("Failed to refresh MediaTailor session")
        }
    }

    override fun dispose() {
        refreshTrackingResponseJob?.cancel()
        mainScope.cancel()
    }
}
