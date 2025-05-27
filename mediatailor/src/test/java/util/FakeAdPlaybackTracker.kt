package util

import com.bitmovin.player.integration.mediatailor.AdPlaybackTracker
import com.bitmovin.player.integration.mediatailor.PlayingAdBreak
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import kotlinx.coroutines.flow.StateFlow

internal class FakeAdPlaybackTracker(
    override val nextAdBreak: StateFlow<MediaTailorAdBreak?>,
    override val playingAdBreak: StateFlow<PlayingAdBreak?>,
) : AdPlaybackTracker {
    override fun dispose() {
        TODO("Not yet implemented")
    }
}
