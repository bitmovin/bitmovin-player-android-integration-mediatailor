package com.bitmovin.player.integration.mediatailor.api

import com.bitmovin.player.api.Player
import com.bitmovin.player.integration.mediatailor.InternalMediaTailorSessionManager
import kotlinx.coroutines.flow.Flow

/**
 * Manages a MediaTailor session for a given [Player].
 *
 * It is responsible for initializing and stopping the session,
 * as well as providing access to the events emitted by the session.
 *
 * As this is bound to the [Player] lifecycle, it should also be stopped when the player is released,
 * or when loading a new source, using [stopSession].
 *
 * ### Example Usage:
 * ```kotlin
 * val player = Player(context)
 * val mediaTailorSessionManager = MediaTailorSessionManager(player)
 *
 * scope.launch {
 *     val sessionResult = mediaTailorSessionManager.initializeSession(
 *         MediaTailorSessionConfig(
 *             sessionInitUrl = "https://example.com/mediatailor/session-init.m3u8",
 *             assetType = MediaTailorAssetType.Linear(),
 *         )
 *     )
 *     when (val sessionResult = sessionResult) {
 *         is Success -> {
 *             player.load(SourceConfig.fromUrl(sessionResult.manifestUrl))
 *         }
 *         is Failure -> {
 *              // Handle session initialization failure
 *         }
 *     }
 * }
 *
 * // Stop the session when no longer needed:
 * mediaTailorSessionManager.destroy()
 * player.destroy()
 * ```
 *
 * ### Limitations:
 * - Playlists are currently not supported
 */
public interface MediaTailorSessionManager {
    /**
     * A flow that emits events related to the [MediaTailorSessionManager] and all it's sessions.
     * For all the event types, see [MediaTailorEvent].
     *
     * For example to listen for ad break started events:
     * ```kotlin
     * mediaTailorSessionManager.events.filterIsInstance<MediaTailorEvent.AdBreakStarted>().collect {}
     * ```
     */
    public val events: Flow<MediaTailorEvent>

    /**
     * Initializes a MediaTailor session with the given configuration.
     * [sessionConfig] provides configuration for the MediaTailor session.
     *
     * Returns [SessionInitializationResult] indicating either:
     * - [SessionInitializationResult.Success] for successful initialization
     * - [SessionInitializationResult.Failure] for any errors during initialization
     */
    public suspend fun initializeSession(sessionConfig: MediaTailorSessionConfig): SessionInitializationResult

    /**
     * Send a [TrackingEvent] for currently playing ad.
     * If there is no currently playing ad or the current ad doesn't contain this tracking event,
     * it will be ignored.
     *
     * Note that [MediaTailorSessionManager] sends the following tracking events automatically:
     * - [PlayerTrackingEvents]
     * - [LinearAdTrackingEvents]
     */
    public fun sendTrackingEvent(event: TrackingEvent)

    /**
     * Stops the current MediaTailor session, if any.
     * This will clean up resources and stop any ongoing ad tracking or beaconing.
     */
    public fun stopSession()

    /**
     * Stops the current session and destroys the [MediaTailorSessionManager].
     * This should be called when the session manager is no longer needed,
     * non of the [MediaTailorSessionManager] methods should be called after this.
     */
    public fun destroy()
}

/**
 * Creates a [MediaTailorSessionManager] for the given [Player].
 */
public fun MediaTailorSessionManager(player: Player): MediaTailorSessionManager = InternalMediaTailorSessionManager(
    player = player,
)
