package com.bitmovin.player.integration.mediatailor.eventEmitter

import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

internal interface InternalEventEmitter {
    val events: SharedFlow<MediaTailorEvent>
    fun emit(event: MediaTailorEvent)
}

internal class FlowEventEmitter : InternalEventEmitter {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _events = MutableSharedFlow<MediaTailorEvent>()
    override val events: SharedFlow<MediaTailorEvent>
        get() = _events

    override fun emit(event: MediaTailorEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}
