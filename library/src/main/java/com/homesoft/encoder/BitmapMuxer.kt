package com.homesoft.encoder

import android.media.MediaCodecList
import android.media.MediaCodecList.REGULAR_CODECS
import android.media.MediaFormat

class BitmapMuxer {

    private var muxerConfig: MuxerConfig = MuxerConfig()

    fun build(config: MuxerConfig) {
        muxerConfig = config
    }

    fun mux(audioTrack: Int, images: Array<Int>) {
        // Returns on a callback a finished video
    }

    fun isSupported(mimeType: String?): Boolean {
        val codecs = MediaCodecList(REGULAR_CODECS)
        for (codec in codecs.codecInfos) {
            if (!codec.isEncoder) {
                continue
            }
            for (type in codec.supportedTypes) {
                if (type == mimeType) return true
            }
        }
        return false
    }

}

data class MuxerConfig(
        var codec: Codec = HevcCodec(),
        var filePath: String = "",
        var videoWidth: Int = 320,
        var videoHeight: Int = 240,
        var mimeType: String = MediaFormat.MIMETYPE_VIDEO_HEVC,
        var audioTrack: Int? = null,
        var framesPerImage: Int = 1,
        var framesPerSecond: Float = 10F,
        var bitrate: Int = 1500000,
        var frameMuxer: FrameMuxer = Mp4FrameMuxer(filePath, framesPerSecond),
        var iFrameInterval: Int = 10
)
