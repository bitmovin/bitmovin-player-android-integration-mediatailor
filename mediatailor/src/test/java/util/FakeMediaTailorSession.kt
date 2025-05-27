package util

import com.bitmovin.player.integration.mediatailor.MediaTailorSession
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult
import kotlinx.coroutines.flow.StateFlow

class FakeMediaTailorSession(
    override val isInitialized: Boolean = true,
    override val adBreaks: StateFlow<List<MediaTailorAdBreak>>,
) : MediaTailorSession {
    override suspend fun initialize(sessionConfig: MediaTailorSessionConfig): SessionInitializationResult {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }
}
