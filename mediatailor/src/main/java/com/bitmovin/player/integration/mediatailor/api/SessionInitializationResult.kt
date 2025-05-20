package com.bitmovin.player.integration.mediatailor.api

public sealed class SessionInitializationResult {
    class Success(
        val manifestUrl: String,
    ) : SessionInitializationResult()

    class Failure(
        val message: String?,
    ) : SessionInitializationResult()
}
