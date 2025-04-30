package com.bitmovin.player.integration.mediatailor.mediatailorsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.integration.mediatailor.MediaTailorPlayer
import com.bitmovin.player.integration.mediatailor.mediatailorsample.ui.PlayerView
import com.bitmovin.player.integration.mediatailor.mediatailorsample.ui.theme.MediaTailorSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaTailorSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val context = LocalContext.current
                    val player = remember { MediaTailorPlayer(Player(context)) }

                    DisposableEffect(player) {
                        player.load(SourceConfig.fromUrl("https://cdn.bitmovin.com/content/assets/sintel/sintel.mpd"))

                        onDispose {
                            player.destroy()
                        }
                    }

                    PlayerView(
                        Modifier.padding(innerPadding),
                        player
                    )
                }
            }
        }
    }
}
