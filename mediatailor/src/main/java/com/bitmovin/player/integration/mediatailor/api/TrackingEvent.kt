package com.bitmovin.player.integration.mediatailor.api

/**
 * MediaTailor tracking events.
 *
 * See [MediaTailorSessionManager.sendTrackingEvent] for more information on how to trigger event tracking.
 */
public sealed class TrackingEvent(
    public val eventType: String
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
 */
internal sealed class PlayerTrackingEvents(
    eventType: String
) : TrackingEvent(eventType) {
    /**
     * Triggered when the player gets muted
     */
    object Mute : PlayerTrackingEvents("mute")

    /**
     * Triggered when the player gets unmuted
     */
    object Unmute : PlayerTrackingEvents("unmute")

    /**
     * Triggered when the player resumes after being paused
     */
    object Resume : PlayerTrackingEvents("resume")

    /**
     * Triggered when the player is paused
     */
    object Pause : PlayerTrackingEvents("pause")

    /**
     * Triggered when the player enters fullscreen mode
     */
    object Fullscreen : PlayerTrackingEvents("fullscreen")

    /**
     * Triggered when the player exits fullscreen mode
     */
    object ExitFullscreen : PlayerTrackingEvents("exitFullscreen")
}

/**
 * Tracking events related to linear ad playback.
 *
 * These events are automatically tracked by [MediaTailorSessionManager],
 * when the player reaches a time that matches [MediaTailorTrackingEvent.scheduleTime]
 * and the [MediaTailorLinearAd.trackingEvents] contains the event type.
 */
internal sealed class LinearAdTrackingEvents(
    eventType: String
) : TrackingEvent(eventType) {
    object Loaded : LinearAdTrackingEvents("loaded")
    object Start : LinearAdTrackingEvents("start")
    object FirstQuartile : LinearAdTrackingEvents("firstQuartile")
    object Midpoint : LinearAdTrackingEvents("midpoint")
    object ThirdQuartile : LinearAdTrackingEvents("thirdQuartile")
    object Complete : LinearAdTrackingEvents("complete")
    object Progress : LinearAdTrackingEvents("progress")
    object Impression : LinearAdTrackingEvents("impression")
}
