package com.bitmovin.player.integration.mediatailor.mediatailorsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.bitmovin.player.api.DebugConfig
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
                    Box(Modifier.padding(innerPadding)) {
                        PlaybackScreen()
                    }
                }
            }
        }
    }
}
