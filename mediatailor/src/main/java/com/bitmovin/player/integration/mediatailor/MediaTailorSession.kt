package com.bitmovin.player.integration.mediatailor

import android.util.Log
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.integration.mediatailor.model.ImplicitSessionStartResponse
import com.bitmovin.player.integration.mediatailor.model.MediaTailorTrackingResponse
import com.bitmovin.player.integration.mediatailor.network.DefaultHttpClient
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
}

internal class DefaultMediaTailorSession(
    private val httpClient: HttpClient = DefaultHttpClient(),
) : MediaTailorSession {
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val _trackingUrl = MutableStateFlow<String?>(null)
    private val _trackingResponse = MutableStateFlow<MediaTailorTrackingResponse?>(null)
    private val mainScope = CoroutineScope(Dispatchers.Main)
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
                response?.let {
                    Log.d(TAG, "Tracking Response: $it")
                    val adBreaks = it.avails.map {
                        it.availId to (it.startTimeInSeconds to it.startTimeInSeconds + it.durationInSeconds)
                    }
                    Log.d(TAG, "Ad Breaks: $adBreaks")
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
