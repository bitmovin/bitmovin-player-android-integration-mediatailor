package com.bitmovin.player.integration.mediatailor.util

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.on
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal inline fun <reified T : Event> Player.eventFlow(): Flow<T> = callbackFlow {
    val listener: (T) -> Unit = { event ->
        trySend(event)
    }
    on<T>(listener)
    awaitClose { off(listener) }
}
