package com.bitmovin.player.integration.mediatailor.api

/**
 * Represents the result of a session initialization attempt.
 * Check [Success] and [Failure] for details.
 */
public sealed class SessionInitializationResult {
    /**
     * Indicates that the session was successfully initialized.
     *
     * Use [manifestUrl] to access the URL of the manifest and load it into the player.
     */
    public class Success(
        public val manifestUrl: String,
    ) : SessionInitializationResult()

    /**
     * Indicates that the session initialization failed.
     *
     * Check [message] for more details about the failure.
     */
    public class Failure(
        public val message: String?,
    ) : SessionInitializationResult()
}
