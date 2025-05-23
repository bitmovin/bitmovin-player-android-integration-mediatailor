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
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bitmovin.player.api.DebugConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAdBreak
import com.bitmovin.player.integration.mediatailor.api.MediaTailorAssetType
import com.bitmovin.player.integration.mediatailor.api.MediaTailorEvent
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionConfig
import com.bitmovin.player.integration.mediatailor.api.MediaTailorSessionManager
import com.bitmovin.player.integration.mediatailor.api.SessionInitializationResult
import com.bitmovin.player.integration.mediatailor.mediatailorsample.ui.PlayerView
import com.bitmovin.player.integration.mediatailor.mediatailorsample.ui.theme.MediaTailorSampleTheme
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
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
                    val sessionManager = remember { MediaTailorSessionManager(player) }
                    val scope = rememberCoroutineScope()
                    val nextAdBreak: State<MediaTailorAdBreak?> = sessionManager
                        .events
                        .filterIsInstance<MediaTailorEvent.UpcomingAdBreakUpdate>()
                        .map { it.adBreak }
                        .collectAsState(null)
                    val currentAdBreak: State<MediaTailorAdBreak?> = sessionManager
                        .events
                        .filter { it is MediaTailorEvent.AdBreakStarted || it is MediaTailorEvent.AdBreakFinished }
                        .map {
                            if (it is MediaTailorEvent.AdBreakStarted) {
                                it.adBreak
                            } else {
                                null
                            }
                        }
                        .collectAsState(null)
                    val currentAd: State<MediaTailorEvent.AdStarted?> = sessionManager
                        .events
                        .filter { it is MediaTailorEvent.AdStarted || it is MediaTailorEvent.AdFinished }
                        .map { it as? MediaTailorEvent.AdStarted }
                        .collectAsState(null)
                    var nextAdBreakInSeconds by remember { mutableStateOf<Int?>(null) }
                    var currentAdBreakSecondsLeft by remember { mutableStateOf<Int?>(null) }
                    var currentAdSecondsLeft by remember { mutableStateOf<Int?>(null) }

                    DisposableEffect(player) {
                        player.on<PlayerEvent.TimeChanged> {
                            if (nextAdBreak.value != null) {
                                nextAdBreakInSeconds =
                                    (nextAdBreak.value!!.scheduleTime - player.currentTime).toInt()
                            }
                            if (currentAdBreak.value != null) {
                                currentAdBreakSecondsLeft =
                                    (currentAdBreak.value!!.let { it.scheduleTime + it.duration } - player.currentTime).toInt()
                            }
                            if (currentAd.value != null) {
                                currentAdSecondsLeft =
                                    (currentAd.value!!.let { it.ad.scheduleTime + it.ad.duration } - player.currentTime).toInt()
                            }
                        }
                        scope.launch {
                            val sessionInitResult = sessionManager.initializeSession(
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
                            sessionManager.events.collect { event ->
                                when (event) {
                                    is MediaTailorEvent.AdBreakStarted -> {
                                        Log.d("MainActivity", "AdBreak started: $event")
                                    }

                                    is MediaTailorEvent.AdBreakFinished -> {
                                        Log.d("MainActivity", "AdBreak ended: $event")
                                    }

                                    is MediaTailorEvent.AdStarted -> {
                                        Log.d("MainActivity", "Ad started: $event")
                                    }

                                    is MediaTailorEvent.AdFinished -> {
                                        Log.d("MainActivity", "Ad ended: $event")
                                    }

                                    is MediaTailorEvent.UpcomingAdBreakUpdate -> {
                                        Log.d("MainActivity", "Upcoming ad break: $event")
                                    }

                                    is MediaTailorEvent.Info -> {
                                        Log.i("MainActivity", "Info: $event")
                                    }

                                    is MediaTailorEvent.Error -> {
                                        Log.e("MainActivity", "Error: $event")
                                    }
                                }
                            }
                        }

                        onDispose {
                            sessionManager.destroy()
                            player.destroy()
                        }
                    }

                    Column {
                        PlayerView(
                            Modifier.padding(innerPadding),
                            player
                        )
                        if (nextAdBreak.value != null) {
                            Text("Next ad break in ${nextAdBreakInSeconds}s")
                        }
                        if (currentAdBreak.value != null) {
                            Text("Current ad break: ${currentAdBreakSecondsLeft}s left.")
                        }
                        if (currentAd.value != null) {
                            Text("Current ad: ${currentAd.value!!.indexInQueue + 1} / ${currentAdBreak.value!!.ads.size} - ${currentAdSecondsLeft}s left.")
                        }
                    }
                }
            }
        }
    }
}
