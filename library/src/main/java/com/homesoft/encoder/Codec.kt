package com.homesoft.encoder

import android.media.MediaCodecInfo
import android.media.MediaFormat

abstract class Codec {

    val IFRAME_INTERVAl = 10

    abstract val MIME_TYPE: String

    fun getVideoMediaFormat(): MediaFormat {
        val format = MediaFormat.createVideoFormat(AvcEncoderConfig.MIME_TYPE, getWidth(), getHeight())

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, getBitRate())
        format.setFloat(MediaFormat.KEY_FRAME_RATE, getFramePerSecond())
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, AvcEncoderConfig.IFRAME_INTERVAL)

        return format
    }

}


class HevcCodec: Codec() {

    override val MIME_TYPE: String = MediaFormat.MIMETYPE_VIDEO_HEVC

}

class AvcCodec: Codec() {

    override val MIME_TYPE: String = MediaFormat.MIMETYPE_VIDEO_AVC

}
