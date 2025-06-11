package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.eventEmitter.InternalEventEmitter
import com.bitmovin.player.integration.mediatailor.util.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal interface MediaTailorSessionEventEmitter : Disposable

internal class DefaultMediaTailorSessionEventEmitter(
    private val mediaTailorSession: MediaTailorSession,
    private val eventEmitter: InternalEventEmitter,
) : MediaTailorSessionEventEmitter {
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            mediaTailorSession.adBreaks.collect {
                eventEmitter.emit(MediaTailorEvent.AdBreakScheduleUpdated(it))
            }
        }
    }

    override fun dispose() {
        scope.cancel()
    }
}
