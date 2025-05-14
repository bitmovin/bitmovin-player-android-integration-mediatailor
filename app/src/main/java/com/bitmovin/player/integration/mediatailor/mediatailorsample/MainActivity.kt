package com.bitmovin.player.integration.mediatailor.mediatailorsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bitmovin.player.api.DebugConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.source.SourceType
import com.bitmovin.player.integration.mediatailor.MediaTailorAssetType
import com.bitmovin.player.integration.mediatailor.MediaTailorPlayer
import com.bitmovin.player.integration.mediatailor.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.MediaTailorSourceConfig
import com.bitmovin.player.integration.mediatailor.mediatailorsample.ui.PlayerView
import com.bitmovin.player.integration.mediatailor.mediatailorsample.ui.theme.MediaTailorSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // TODO: Remove
        DebugConfig.isLoggingEnabled = true

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaTailorSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val context = LocalContext.current
                    val player = remember { MediaTailorPlayer(Player(context)) }

                    DisposableEffect(player) {
                        val source = MediaTailorSourceConfig(
                            sessionConfig = MediaTailorSessionConfig(
                                sessionInitUrl = "https://awslive.streamco.video/v1/session/86dfd1144b3bf786fc967f2c3876972e5548ca5d/awslive/out/v1/live/jdub-live-bitmovin01/cmaf-cbcs/hls.m3u8",
                                assetType = MediaTailorAssetType.Linear,
                            ),
                            type = SourceType.Hls
                        )
                        player.load(source)

                        onDispose {
                            player.destroy()
                        }
                    }

                    Column {
                        PlayerView(
                            Modifier.padding(innerPadding),
                            player
                        )
                    }
                }
            }
        }
    }
}
