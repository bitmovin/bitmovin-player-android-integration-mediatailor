package com.bitmovin.player.integration.mediatailor.api

public sealed class SessionInitializationResult {
    public class Success(
        public val manifestUrl: String,
    ) : SessionInitializationResult()

    public class Failure(
        public val message: String?,
    ) : SessionInitializationResult()
}
