package com.bitmovin.player.integration.mediatailor.util

import com.bitmovin.player.api.Player
import com.bitmovin.player.integration.mediatailor.AdBeaconing
import com.bitmovin.player.integration.mediatailor.AdPlaybackEventEmitter
import com.bitmovin.player.integration.mediatailor.AdPlaybackTracker
import com.bitmovin.player.integration.mediatailor.AdsMapper
import com.bitmovin.player.integration.mediatailor.DefaultAdBeaconing
import com.bitmovin.player.integration.mediatailor.DefaultAdPlaybackEventEmitter
import com.bitmovin.player.integration.mediatailor.DefaultAdPlaybackTracker
import com.bitmovin.player.integration.mediatailor.DefaultAdsMapper
import com.bitmovin.player.integration.mediatailor.DefaultMediaTailorSession
import com.bitmovin.player.integration.mediatailor.DefaultMediaTailorSessionEventEmitter
import com.bitmovin.player.integration.mediatailor.MediaTailorSession
import com.bitmovin.player.integration.mediatailor.MediaTailorSessionEventEmitter
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.eventEmitter.DefaultEventEmitter
import com.bitmovin.player.integration.mediatailor.eventEmitter.InternalEventEmitter
import com.bitmovin.player.integration.mediatailor.network.DefaultHttpClient
import com.bitmovin.player.integration.mediatailor.network.HttpClient
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope

internal class DependencyFactory {
    fun createAdBeaconing(
        player: Player,
        adPlaybackTracker: AdPlaybackTracker,
        httpClient: HttpClient,
        eventEmitter: InternalEventEmitter,
        sessionConfig: MediaTailorSessionConfig,
    ): AdBeaconing = DefaultAdBeaconing(
        player,
        adPlaybackTracker,
        httpClient,
        eventEmitter,
        sessionConfig,
    )

    fun createAdPlaybackEventEmitter(
        adPlaybackTracker: AdPlaybackTracker,
        eventEmitter: InternalEventEmitter,
    ): AdPlaybackEventEmitter = DefaultAdPlaybackEventEmitter(
        adPlaybackTracker,
        eventEmitter,
    )

    fun createMediaTailorSessionEventEmitter(
        mediaTailorSession: MediaTailorSession,
        eventEmitter: InternalEventEmitter,
    ): MediaTailorSessionEventEmitter = DefaultMediaTailorSessionEventEmitter(
        mediaTailorSession,
        eventEmitter,
    )

    fun createAdPlaybackTracker(player: Player, mediaTailorSession: MediaTailorSession): AdPlaybackTracker =
        DefaultAdPlaybackTracker(
            player,
            mediaTailorSession,
        )

    fun createMediaTailorSession(player: Player, httpClient: HttpClient, adsMapper: AdsMapper): MediaTailorSession =
        DefaultMediaTailorSession(
            player,
            httpClient,
            adsMapper,
        )

    fun createAdsMapper(): AdsMapper = DefaultAdsMapper()
    fun createHttpClient(): HttpClient = DefaultHttpClient()
    fun createAdMapper(): AdsMapper = DefaultAdsMapper()
    fun createEventEmitter(): InternalEventEmitter = DefaultEventEmitter()
    fun createScope(context: CoroutineContext): CoroutineScope = CoroutineScope(context)
}
