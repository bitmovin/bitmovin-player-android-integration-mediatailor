package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.integration.mediatailor.model.ImplicitSessionStartResponse
import com.bitmovin.player.integration.mediatailor.model.MediaTailorTrackingSession
import com.bitmovin.player.integration.mediatailor.network.DataSourceFactory
import kotlinx.serialization.json.Json

internal interface MediaTailorSessionInitInitializer {
    suspend fun prepareImplicitSession(
        sessionConfig: MediaTailorSessionConfig.Implicit
    ): ImplicitSessionStartResponse

    suspend fun initialize(
        sessionConfig: MediaTailorSessionConfig.Explicit
    ): MediaTailorTrackingSession

    suspend fun refresh(trackingUrl: String): MediaTailorTrackingSession
}

internal class DefaultMediaTailorSessionInitInitializer(
    private val dataSourceFactory: DataSourceFactory,
) : MediaTailorSessionInitInitializer {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun prepareImplicitSession(
        sessionConfig: MediaTailorSessionConfig.Implicit
    ): ImplicitSessionStartResponse {
        val response = dataSourceFactory.create(sessionConfig.sessionInitUrl).post()
        return if (response != null) {
            json.decodeFromString<ImplicitSessionStartResponse>(response)
        } else {
            throw IllegalStateException("Failed to initialize MediaTailor session")
        }
    }

    override suspend fun initialize(
        sessionConfig: MediaTailorSessionConfig.Explicit
    ): MediaTailorTrackingSession {
        val response = dataSourceFactory.create(sessionConfig.trackingUrl).get()
        return if (response != null) {
            json.decodeFromString<MediaTailorTrackingSession>(response)
        } else {
            throw IllegalStateException("Failed to initialize MediaTailor session")
        }
    }

    override suspend fun refresh(trackingUrl: String): MediaTailorTrackingSession {
        val response = dataSourceFactory.create(trackingUrl).get()
        return if (response != null) {
            json.decodeFromString<MediaTailorTrackingSession>(response)
        } else {
            throw IllegalStateException("Failed to refresh MediaTailor session")
        }
    }
}

sealed class MediaTailorSessionConfig {
    data class Implicit(
        val sessionInitUrl: String,
    ) : MediaTailorSessionConfig()

    data class Explicit(
        val manifestUrl: String,
        val trackingUrl: String,
    ) : MediaTailorSessionConfig()
}