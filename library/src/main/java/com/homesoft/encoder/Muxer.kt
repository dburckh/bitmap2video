package com.homesoft.encoder

import android.R
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaCodecList
import android.media.MediaCodecList.REGULAR_CODECS
import android.media.MediaFormat
import android.util.Log
import androidx.annotation.RawRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import kotlinx.coroutines.launch

class Muxer(
        private val context: Context,
        private val filePath: String,
        private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    companion object {
        private val TAG = Muxer::class.java.simpleName
    }

    // Initialize a default configuration
    private var muxerConfig: MuxerConfig = MuxerConfig(filePath)
    private var muxingCompletionListener: MuxingCompletionListener? = null

    /**
     * Build the BitmapMuxer with a custom [MuxerConfig]
     *
     * @param config: muxer configuration object
     */
    fun build(config: MuxerConfig) {
        muxerConfig = config
    }

    /**
     * List containing images in any of the following formats:
     * [Bitmap] [@DrawRes Int] [Canvas]
     */
    fun mux(imageList: List<Any>,
            @RawRes audioTrack: Int? = null) {
        // Returns on a callback a finished video
        Log.d(TAG, "Generating video")
        val frameBuilder = FrameBuilder(context, muxerConfig, audioTrack)

        try {
            frameBuilder.start()
        } catch (e: IOException) {
            Log.e(TAG, "Start Encoder Failed")
            e.printStackTrace()
            muxingCompletionListener?.onVideoError(e)
        }

        for (image in imageList) {
            frameBuilder.createFrame(image)
        }

        // Release the video codec so we can mux in the audio frames separately
        frameBuilder.releaseVideoCodec()

        // Add audio
        frameBuilder.muxAudioFrames()

        // Release everything
        frameBuilder.releaseAudioExtractor()
        frameBuilder.releaseMuxer()

        muxingCompletionListener?.onVideoSuccessful(filePath)
    }

    suspend fun muxAsync(imageList: List<Any>, @RawRes audioTrack: Int? = null) {
        coroutineScope.launch(Dispatchers.Default) {
            mux(imageList, audioTrack)
        }
    }

    fun setOnMuxingCompleted(muxingCompletionListener: MuxingCompletionListener) {
        this.muxingCompletionListener = muxingCompletionListener
    }
}

fun isCodecSupported(mimeType: String?): Boolean {
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

interface MuxingCompletionListener {
    fun onVideoSuccessful(filepath: String)
    fun onVideoError(error: Throwable)
}

data class MuxerConfig(
        var filePath: String,
        var videoWidth: Int = 320,
        var videoHeight: Int = 240,
        var mimeType: String = MediaFormat.MIMETYPE_VIDEO_HEVC,
        var framesPerImage: Int = 1,
        var framesPerSecond: Float = 10F,
        var bitrate: Int = 1500000,
        var frameMuxer: FrameMuxer = Mp4FrameMuxer(filePath, framesPerSecond),
        var iFrameInterval: Int = 10
)
