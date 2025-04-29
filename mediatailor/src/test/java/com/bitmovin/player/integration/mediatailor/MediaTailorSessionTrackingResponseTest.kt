package com.bitmovin.player.integration.mediatailor

import kotlinx.serialization.json.Json
import org.junit.Test

class MediaTailorSessionTrackingResponseTest {
    @Test
    fun `parses sample MediaTailor session tracking response`() {
        Json.decodeFromString<MediaTailorSessionTrackingResponse>(sampleMediaTailorTrackingResponse)
    }
}

private val sampleMediaTailorTrackingResponse = """
            {
              "avails": [
                {
                  "ads": [
                    {
                      "adId": "8104385",
                      "duration": "PT15.100000078S",
                      "durationInSeconds": 15.1,
                      "startTime": "PT17.817798612S",
                      "startTimeInSeconds": 17.817,
                      "trackingEvents": [
                      {
                          "beaconUrls": [
                            "http://exampleadserver.com/tracking?event=impression"
                          ],
                          "duration": "PT15.100000078S",
                          "durationInSeconds": 15.1,
                          "eventId": "8104385",
                          "eventType": "impression",
                          "startTime": "PT17.817798612S",
                          "startTimeInSeconds": 17.817
                        },
                        {
                          "beaconUrls": [
                            "http://exampleadserver.com/tracking?event=start"
                          ],
                          "duration": "PT0S",
                          "durationInSeconds": 0.0,
                          "eventId": "8104385",
                          "eventType": "start",
                          "startTime": "PT17.817798612S",
                          "startTimeInSeconds": 17.817
                        },
                        {
                          "beaconUrls": [
                            "http://exampleadserver.com/tracking?event=firstQuartile"
                          ],
                          "duration": "PT0S",
                          "durationInSeconds": 0.0,
                          "eventId": "8104386",
                          "eventType": "firstQuartile",
                          "startTime": "PT21.592798631S",
                          "startTimeInSeconds": 21.592
                        },
                         {
                          "beaconUrls": [
                            "http://exampleadserver.com/tracking?event=midpoint"
                          ],
                          "duration": "PT0S",
                          "durationInSeconds": 0.0,
                          "eventId": "8104387",
                          "eventType": "midpoint",
                          "startTime": "PT25.367798651S",
                          "startTimeInSeconds": 25.367
                        },
                        {
                          "beaconUrls": [
                            "http://exampleadserver.com/tracking?event=thirdQuartile"
                          ],
                          "duration": "PT0S",
                          "durationInSeconds": 0.0,
                          "eventId": "8104388",
                          "eventType": "thirdQuartile",
                          "startTime": "PT29.14279867S",
                          "startTimeInSeconds": 29.142
                        },
                        {
                          "beaconUrls": [
                            "http://exampleadserver.com/tracking?event=complete"
                          ],
                          "duration": "PT0S",
                          "durationInSeconds": 0.0,
                          "eventId": "8104390",
                          "eventType": "complete",
                          "startTime": "PT32.91779869S",
                          "startTimeInSeconds": 32.917
                        }
                      ]
                    }
                  ],
                  "availId": "8104385",
                  "duration": "PT15.100000078S",
                  "durationInSeconds": 15.1,
                  "startTime": "PT17.817798612S",
                  "startTimeInSeconds": 17.817
                }
              ]
            }
        """.trimIndent()