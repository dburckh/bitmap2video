package com.homesoft.encoder

import android.media.MediaCodecList
import android.media.MediaCodecList.REGULAR_CODECS

class BitmapMuxer {

    var muxerConfig: MuxerConfig = MuxerConfig()

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
                if (type.equals(mimeType, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

}

data class MuxerConfig(
        var codec: Codec,
        var filePath: String,
        var videoWidth: Int,
        var videoHeight: Int,
        var mimeType: String,
        var audioTrack: Int,
        var framesPerImage: Int
)
