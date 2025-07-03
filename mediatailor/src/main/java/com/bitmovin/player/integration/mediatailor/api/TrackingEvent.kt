package com.bitmovin.player.integration.mediatailor.api

/**
 * MediaTailor tracking events.
 *
 * See [MediaTailorSessionManager.sendTrackingEvent] for more information on how to trigger event tracking.
 */
public sealed class TrackingEvent(
    public val eventType: String,
) {
    /**
     * Tracks that the user has clicked on an ad.
     */
    public object ClickTracking : TrackingEvent("clickTracking")

    /**
     * This class can be used to track any events that are not implemented yet.
     */
    public class Other(eventType: String) : TrackingEvent(eventType)
}

/**
 * Tracking events related to player events.
 *
 * These events are automatically tracked by [MediaTailorSessionManager].
 *
 * Use [MediaTailorSessionConfig.automaticAdTrackingEnabled] in  case you want to disable automatic
 * tracking and send this events manually using [MediaTailorSessionManager.sendTrackingEvent].
 */
public sealed class PlayerTrackingEvents(
    eventType: String,
) : TrackingEvent(eventType) {
    /**
     * Triggered when the player gets muted
     */
    public object Mute : PlayerTrackingEvents("mute")

    /**
     * Triggered when the player gets unmuted
     */
    public object Unmute : PlayerTrackingEvents("unmute")

    /**
     * Triggered when the player resumes after being paused
     */
    public object Resume : PlayerTrackingEvents("resume")

    /**
     * Triggered when the player is paused
     */
    public object Pause : PlayerTrackingEvents("pause")

    /**
     * Triggered when the player enters fullscreen mode
     */
    public object Fullscreen : PlayerTrackingEvents("fullscreen")

    /**
     * Triggered when the player exits fullscreen mode
     */
    public object ExitFullscreen : PlayerTrackingEvents("exitFullscreen")
}

/**
 * Tracking events related to linear ad playback.
 *
 * These events are automatically tracked by [MediaTailorSessionManager],
 * when the player reaches a time that matches [MediaTailorTrackingEvent.scheduleTime]
 * and the [MediaTailorLinearAd.trackingEvents] contains the event type.
 *
 * Use [MediaTailorSessionConfig.automaticAdTrackingEnabled] in  case you want to disable automatic
 * tracking and send this events manually using [MediaTailorSessionManager.sendTrackingEvent].
 */
public sealed class LinearAdTrackingEvents(
    eventType: String,
) : TrackingEvent(eventType) {
    public object Loaded : LinearAdTrackingEvents("loaded")
    public object Start : LinearAdTrackingEvents("start")
    public object FirstQuartile : LinearAdTrackingEvents("firstQuartile")
    public object Midpoint : LinearAdTrackingEvents("midpoint")
    public object ThirdQuartile : LinearAdTrackingEvents("thirdQuartile")
    public object Complete : LinearAdTrackingEvents("complete")
    public object Progress : LinearAdTrackingEvents("progress")
    public object Impression : LinearAdTrackingEvents("impression")

    public companion object {
        public val values: Set<LinearAdTrackingEvents> = setOf(
            Loaded,
            Start,
            FirstQuartile,
            Midpoint,
            ThirdQuartile,
            Complete,
            Progress,
            Impression,
        )
    }
}
