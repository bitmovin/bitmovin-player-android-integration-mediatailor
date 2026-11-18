package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.integration.mediatailor.model.Ad
import com.bitmovin.player.integration.mediatailor.model.Avail
import com.bitmovin.player.integration.mediatailor.model.TrackingEvent
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Merges a partial (paged) tracking response into the already known avails.
 *
 * Only adds or updates, never removes: an avail, ad or tracking event missing from a paged response only means that
 * there is nothing new for it. The start time and duration of an avail/ad jitter between responses, so avails are
 * matched by `availId`, ads by `adId` and tracking events by `(eventId, eventType, beaconUrls)` (`eventId` is not
 * unique within an ad). On a match, the incoming value wins. The result is sorted by start time.
 */
internal fun mergeAvails(existing: List<Avail>, incoming: List<Avail>): List<Avail> {
    val merged = LinkedHashMap<String, Avail>()
    existing.forEach { merged[it.availId] = it }
    incoming.forEach { incomingAvail ->
        val existingAvail = merged[incomingAvail.availId]
        merged[incomingAvail.availId] = if (existingAvail == null) {
            incomingAvail
        } else {
            incomingAvail.copy(ads = mergeAds(existingAvail.ads, incomingAvail.ads))
        }
    }
    return merged.values.sortedBy { it.startTimeInSeconds }
}

private fun mergeAds(existing: List<Ad>, incoming: List<Ad>): List<Ad> {
    val merged = LinkedHashMap<String, Ad>()
    existing.forEach { merged[it.adId] = it }
    incoming.forEach { incomingAd ->
        val existingAd = merged[incomingAd.adId]
        merged[incomingAd.adId] = if (existingAd == null) {
            incomingAd
        } else {
            incomingAd.copy(trackingEvents = mergeTrackingEvents(existingAd.trackingEvents, incomingAd.trackingEvents))
        }
    }
    return merged.values.sortedBy { it.startTimeInSeconds }
}

private fun mergeTrackingEvents(existing: List<TrackingEvent>, incoming: List<TrackingEvent>): List<TrackingEvent> {
    val merged = LinkedHashMap<Triple<String, String, List<String>>, TrackingEvent>()
    (existing + incoming).forEach { merged[Triple(it.eventId, it.eventType, it.beaconUrls)] = it }
    return merged.values.toList()
}

/**
 * Appends the `aws.nextToken` query parameter to this url, keeping existing query parameters.
 *
 * MediaTailor returns the token sometimes already percent-encoded and sometimes not. It is decoded first and then
 * encoded exactly once, so that base64 characters (`+`, `/`, `=`) are always sent percent-encoded.
 */
internal fun String.withNextToken(nextToken: String): String {
    // URLDecoder turns `+` into a space, but in a base64 token it is a literal plus.
    val decodedToken = URLDecoder.decode(nextToken.replace("+", "%2B"), "UTF-8")
    // URLEncoder encodes a space as `+`, which is not a valid percent-encoding in this context.
    val encodedToken = URLEncoder.encode(decodedToken, "UTF-8").replace("+", "%20")
    val separator = if (contains('?')) "&" else "?"
    return "$this${separator}aws.nextToken=$encodedToken"
}
