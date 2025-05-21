package com.bitmovin.player.integration.mediatailor.mediatailorsample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bitmovin.player.api.DebugConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAssetType
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionManager
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult
import com.bitmovin.player.integration.mediatailor.mediatailorsample.ui.PlayerView
import com.bitmovin.player.integration.mediatailor.mediatailorsample.ui.theme.MediaTailorSampleTheme
import kotlinx.coroutines.launch

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
                    val player = remember { Player(context) }
                    val mediaTailorSessionManager = remember { MediaTailorSessionManager(player) }
                    val scope = rememberCoroutineScope()

                    DisposableEffect(player) {
                        scope.launch {
                            val sessionInitResult = mediaTailorSessionManager.initializeSession(
                                MediaTailorSessionConfig(
                                    sessionInitUrl = "https://awslive.streamco.video/v1/session/86dfd1144b3bf786fc967f2c3876972e5548ca5d/awslive/out/v1/live/jdub-live-bitmovin01/cmaf-cbcs/hls.m3u8",
                                    assetType = MediaTailorAssetType.Linear(),
                                )
                            )
                            when (val sessionInitResult = sessionInitResult) {
                                is SessionInitializationResult.Success -> player.load(
                                    SourceConfig.fromUrl(sessionInitResult.manifestUrl)
                                )

                                is SessionInitializationResult.Failure -> {
                                    Log.e(
                                        "MainActivity",
                                        "Failed to create session: ${sessionInitResult.message}"
                                    )
                                }
                            }
                        }
                        scope.launch {
                            mediaTailorSessionManager.events.collect { event ->
                                when (event) {
                                    is MediaTailorEvent.AdBreakStarted -> {
                                        Log.d("MainActivity", "AdBreak started: ${event}")
                                    }

                                    is MediaTailorEvent.AdBreakFinished -> {
                                        Log.d("MainActivity", "AdBreak ended: ${event}")
                                    }

                                    is MediaTailorEvent.AdStarted -> {
                                        Log.d("MainActivity", "Ad started: ${event}")
                                    }

                                    is MediaTailorEvent.AdFinished -> {
                                        Log.d("MainActivity", "Ad ended: ${event}")
                                    }

                                    is MediaTailorEvent.Info -> {
                                        Log.i("MainActivity", "Info: ${event.message}")
                                    }

                                    is MediaTailorEvent.Error -> {
                                        Log.e("MainActivity", "Error: ${event.message}")
                                    }

                                    is MediaTailorEvent.UpcomingAdBreakUpdate -> {
                                        Log.d("MainActivity", "Upcoming ad break: ${event.adBreak}")
                                    }

                                    else -> Unit
                                }
                            }
                        }

                        onDispose {
                            mediaTailorSessionManager.destroy()
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
