package com.bitmovin.player.integration.mediatailor.eventEmitter

import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

internal interface InternalEventEmitter {
    suspend fun emit(event: MediaTailorEvent)
}

internal interface FlowEventEmitter : InternalEventEmitter {
    val events: SharedFlow<MediaTailorEvent>
}

internal class DefaultEventEmitter : FlowEventEmitter {
    private val _events = MutableSharedFlow<MediaTailorEvent>()
    override val events: SharedFlow<MediaTailorEvent>
        get() = _events

    override suspend fun emit(event: MediaTailorEvent) {
        _events.emit(event)
    }
}

internal fun InternalEventEmitter.asFlowEventEmitter(): FlowEventEmitter {
    return this as? FlowEventEmitter ?: throw IllegalStateException(
        "InternalEventEmitter must be an instance of FlowEventEmitter",
    )
}
