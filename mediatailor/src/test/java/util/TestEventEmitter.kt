package util

import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.eventEmitter.InternalEventEmitter

internal class TestEventEmitter : InternalEventEmitter {
    val emittedEvents = mutableListOf<MediaTailorEvent>()

    override fun emit(event: MediaTailorEvent) {
        emittedEvents.add(event)
    }
}