# Example MediaTailor Integration with the Bitmovin Player
## Getting Started
Add your Bitmovin license in the `PlaybackViewModel.kt`:
```kotlin
val player = Player(application, PlayerConfig(key = TODO("PLAYER_LICENSE_KEY")))
```

Configure the session initialization URL and asset type in `PlaybackViewModel.kt`:
```kotlin
val sessionResult = mediaTailorSessionManager.initializeSession(
    MediaTailorSessionConfig(
        sessionInitUrl = TODO("SESSION_INIT_URL"),
        assetType = MediaTailorAssetType.Linear(),
    )
)
```

## Running the Example
Run the `app` configuration.
