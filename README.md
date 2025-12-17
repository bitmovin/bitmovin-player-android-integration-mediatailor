# Bitmovin player MediaTailor integration
This is an open-source project to enable the use of a third-party component (MediaTailor) with the [Bitmovin Player Android SDK](https://bitmovin.com/video-player/android-sdk).

## Maintenance and update
This project is not part of a regular maintenance or update schedule and is updated once yearly to conform with the latest product versions. For additional update requests, please take a look at the guidance further below.

## Contributions to this project
We are pleased to accept changes, updates, and fixes from the community wishing to use and expand this project. Bitmovin will review any Pull Requests made. We do our best to provide timely feedback, but please note that no SLAs apply. New releases are tagged and published at our discretion. Please see [CONTRIBUTING.md](CONTRIBUTING.md) for more details on how to contribute.

## Reporting MediaTailor integration bugs
If you come across a bug related to the Player, please raise this through the support ticketing system accessible in your [Bitmovin Dashboard](https://dashboard.bitmovin.com/support/tickets).

## Support and SLA disclaimer
As an open-source project and not a core product offering, any request, issue, or query related to this project is excluded from any SLA and support terms that a customer might have with either Bitmovin or another third-party service provider or company contributing to this project. Any and all updates are purely at the contributor's discretion.

## Need more help?
Should you want some help updating this project (modifying, fixing, or otherwise) and can't contribute for any reason, please raise your request to your Bitmovin account team, who can discuss your request.

Thank you for your contributions!

## Supported features & workflows
### Stream types
- HLS Live
- Dash Live
- HLS VOD
- DASH VOD
### Beaconing
This integration utilizes MediaTailor's [client-side tracking](https://docs.aws.amazon.com/mediatailor/latest/ug/ad-reporting-client-side.html) for ad tracking data. This integration will handle the sending of the ad beacons provided by MediaTailor's client-side tracking JSON.
Automatically tracked events:
- [PlayerTrackingEvents](https://github.com/bitmovin/bitmovin-player-android-integration-mediatailor/blob/main/mediatailor/src/main/java/com/bitmovin/player/integration/mediatailor/api/TrackingEvent.kt)
- [LinearAdTrackingEvents](https://github.com/bitmovin/bitmovin-player-android-integration-mediatailor/blob/main/mediatailor/src/main/java/com/bitmovin/player/integration/mediatailor/api/TrackingEvent.kt)

It's also possible to manually send tracking events using [MediaTailorSessionManager.sendTrackingEvent](https://github.com/bitmovin/bitmovin-player-android-integration-mediatailor/blob/main/mediatailor/src/main/java/com/bitmovin/player/integration/mediatailor/api/MediaTailorSessionManager.kt)
Check out [TrackingEvent](https://github.com/bitmovin/bitmovin-player-android-integration-mediatailor/blob/main/mediatailor/src/main/java/com/bitmovin/player/integration/mediatailor/api/TrackingEvent.kt) to see which events are supported.

## Limitations
- No support for companion ads
- No support for the Bitmovin player playlist feature
- No support for MediaTailorAdBreak (Avail) adBreakTrackingEvents, currently only MediaTailorAd (Ad) trackingEvents are used for tracking

# Getting started
Follow the [getting started guide](https://developer.bitmovin.com/playback/docs/getting-started-android) to integrate Bitmovin player into your project.
### Gradle configuration
Include Bitmovin's Maven repository in your `settings.gradle.kts` file:
```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://artifacts.bitmovin.com/artifactory/public-releases")
        }
    }
}
```
Include the MediaTailor integration in your `build.gradle.kts` file:
```kotlin
dependencies {
    implementation("com.bitmovin.player.integration:mediatailor:0.1.3")
}
```
## Examples
### Basic session initialization
To create a session and load the stream into the player:
```kotlin
val player = Player(context)
val mediaTailorSessionManager = MediaTailorSessionManager(player)
scope.launch {
    val sessionResult = mediaTailorSessionManager.initializeSession(
        MediaTailorSessionConfig(
            sessionInitUrl = "https://example.com/mediatailor/session-init.m3u8",
            assetType = MediaTailorAssetType.Linear(),
        )
    )
    when (val sessionResult = sessionResult) {
        is Success -> {
            player.load(SourceConfig.fromUrl(sessionResult.manifestUrl))
        }
        is Failure -> {
             // Handle session initialization failure
        }
    }
}
```
Once the session is no longer needed, you can stop it. Always stop the session before initializing a new one.
```kotlin
mediaTailorSessionManager.stopSession()
```
Destroy the session manager when it's no longer needed. After this call, the session manager cannot be used anymore.
Destroying the session manager automatically stops the active session, so there is no need to call `stopSession()` before calling `destroy()`.
```kotlin
mediaTailorSessionManager.destroy()
```
### Event system
`MediaTailorSessionManager` provides an event system based on [Kotlin's Flow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/) to subscribe to session events.
Check out [MediaTailorEvent](https://github.com/bitmovin/bitmovin-player-android-integration-mediatailor/blob/main/mediatailor/src/main/java/com/bitmovin/player/integration/mediatailor/api/MediaTailorEvent.kt) to see all available events.

For example, to log all `MediaTailorEvent.Info` events:
```kotlin
mediaTailorSessionManager.events.filterIsInstance<MediaTailorEvent.Info>().collect {
    Log.d(TAG, it.message)
}
```

Check out the [app module](https://github.com/bitmovin/bitmovin-player-android-integration-mediatailor/tree/main/app) for an example implementation.
