package util

import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.eventEmitter.InternalEventEmitter
import kotlinx.coroutines.flow.SharedFlow

internal class TestEventEmitter : InternalEventEmitter {
    val emittedEvents = mutableListOf<MediaTailorEvent>()
    override val events: SharedFlow<MediaTailorEvent>
        get() = TODO("Not yet implemented")

    override fun emit(event: MediaTailorEvent) {
        emittedEvents.add(event)
    }
}