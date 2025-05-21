package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

internal interface EventEmitter {
    fun emit(event: MediaTailorEvent)
}

internal class FlowEventEmitter : EventEmitter {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _events = MutableSharedFlow<MediaTailorEvent>()
    val events: SharedFlow<MediaTailorEvent>
        get() = _events

    override fun emit(event: MediaTailorEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
}
