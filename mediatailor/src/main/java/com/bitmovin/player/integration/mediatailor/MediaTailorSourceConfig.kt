package com.bitmovin.player.integration.mediatailor

import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.drm.DrmConfig
import com.bitmovin.player.api.live.SourceLiveConfig
import com.bitmovin.player.api.media.LabelingConfig
import com.bitmovin.player.api.media.subtitle.SubtitleTrack
import com.bitmovin.player.api.media.thumbnail.ThumbnailTrack
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.source.SourceOptions
import com.bitmovin.player.api.source.SourceType
import com.bitmovin.player.api.vr.VrConfig

class MediaTailorSourceConfig(
    /**
     * MediaTailor session configuration.
     */
    val mediaTailorSessionConfig: MediaTailorSessionConfig,
    /**
     * The [SourceType] of the [Source].
     */
    type: SourceType,
    /**
     * The title of the [Source].
     */
    title: String? = null,
    /**
     * The descriptions of the [Source].
     */
    description: String? = null,
    /**
     * The URL pointing to the poster image.
     */
    posterSource: String? = null,
    /**
     * Whether the poster is persistent.
     */
    isPosterPersistent: Boolean = false,
    /**
     * A list of additional [SubtitleTrack]s available for the [Source].
     */
    subtitleTracks: List<SubtitleTrack> = emptyList(),
    /**
     * The current [ThumbnailTrack] or `null`.
     */
    thumbnailTrack: ThumbnailTrack? = null,
    /**
     * The current [DrmConfig] or `null`.
     */
    drmConfig: DrmConfig? = null,
    /**
     * The [LabelingConfig] for the [Source].
     */
    labelingConfig: LabelingConfig = LabelingConfig(),
    /**
     * The current [VrConfig].
     */
    vrConfig: VrConfig = VrConfig(),
    /**
     * The video codec priority for the [Source]. First index has the highest priority.
     */
    videoCodecPriority: List<String> = emptyList(),
    /**
     * The audio codec priority for the [Source]. First index has the highest priority.
     */
    audioCodecPriority: List<String> = emptyList(),
    /**
     * The additional [SourceOptions] for the [Source].
     */
    options: SourceOptions = SourceOptions(),
    /**
     * The optional custom metadata. Also sent to the cast receiver when loading the [Source].
     */
    metadata: Map<String, String>? = null,
    /**
     * The [SourceLiveConfig] for the [Source]. Ignored if the [Source] is not a live stream.
     *
     * If this value is `null`, [PlayerConfig.liveConfig] is used instead for backward compatibility.
     */
    liveConfig: SourceLiveConfig? = null,
) : SourceConfig(
    MEDIA_TAILOR_SESSION_NOT_INITIALIZED_YET,
    type,
    title,
    description,
    posterSource,
    isPosterPersistent,
    subtitleTracks,
    thumbnailTrack,
    drmConfig,
    labelingConfig,
    vrConfig,
    videoCodecPriority,
    audioCodecPriority,
    options,
    metadata,
    liveConfig,
)

fun MediaTailorSourceConfig.toSourceConfig(manifestUrl: String): SourceConfig =
    SourceConfig(
        manifestUrl,
        type,
        title,
        description,
        posterSource,
        isPosterPersistent,
        subtitleTracks,
        thumbnailTrack,
        drmConfig,
        labelingConfig,
        vrConfig,
        videoCodecPriority,
        audioCodecPriority,
        options,
        metadata,
        liveConfig
    )

const val MEDIA_TAILOR_SESSION_NOT_INITIALIZED_YET = "MEDIA_TAILOR_SESSION_NOT_INITIALIZED_YET"
