package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.event.Event
import com.bitmovin.player.core.internal.InternalEventEmitter

class MediaTailorEventEmitter(
    private val internalEventEmitter: InternalEventEmitter<Event>
) : InternalEventEmitter<Event> by internalEventEmitter
