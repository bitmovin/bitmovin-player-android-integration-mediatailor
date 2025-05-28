@file:JvmName("PlayerExtension")

package com.bitmovin.player.integration.mediatailor.util

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.Event
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.reflect.KClass

internal inline fun <reified T : Event> Player.eventFlow(): Flow<T> = eventFlow(T::class)

internal fun <T : Event> Player.eventFlow(eventType: KClass<T>): Flow<T> = callbackFlow {
    val listener: (T) -> Unit = { event ->
        trySend(event)
    }
    on(eventType, listener)
    awaitClose { off(eventType, listener) }
}