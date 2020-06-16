package com.homesoft.encoder

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.media.*
import android.media.MediaCodecList.REGULAR_CODECS
import android.os.Build
import android.util.Log
import android.view.Surface
import androidx.annotation.RawRes
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class FrameBuilder(
        private val context: Context,
        private val muxerConfig: MuxerConfig,
        @RawRes private val audioTrackResource: Int?
) {

    private val TAG = FrameBuilder::class.java.simpleName
    private val VERBOSE: Boolean = false
    private val SECOND_IN_USEC = TimeUnit.SECONDS.toMicros(1L)
    private val TIMEOUT_USEC = 10000

    private val mediaFormat: MediaFormat = run {
        val format = MediaFormat.createVideoFormat(muxerConfig.mimeType, muxerConfig
                .videoWidth, muxerConfig.videoHeight)

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, muxerConfig.bitrate)
        format.setFloat(MediaFormat.KEY_FRAME_RATE, muxerConfig.framesPerSecond)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, muxerConfig.iFrameInterval)
        format
    }

    private val mediaCodec: MediaCodec = run {
        val codecs = MediaCodecList(REGULAR_CODECS)
        MediaCodec.createByCodecName(codecs.findEncoderForFormat(mediaFormat))
    }

    private val bufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
    private var surface: Surface = mediaCodec.createInputSurface()
    private var frameMuxer: FrameMuxer = muxerConfig.frameMuxer

    private var rect: Rect? = null

    private var audioExtractor: MediaExtractor? = run {
        if (audioTrackResource != null) {
            val assetFileDescriptor: AssetFileDescriptor = context.resources.openRawResourceFd(audioTrackResource)
            val extractor = MediaExtractor()
            extractor.setDataSource(
                    assetFileDescriptor.fileDescriptor,
                    assetFileDescriptor.startOffset,
                    assetFileDescriptor.length
            )
            extractor
        } else {
            null
        }
    }

    /**
     * @throws IOException
     */
    fun start() {
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec.start()
        drainCodec(false)
    }

    private fun getCanvas(): Canvas? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            surface.lockHardwareCanvas()
        } else {
            surface.lockCanvas(rect)
        }
    }

    fun createFrame(image: Any) {
        when (image) {
            is Int -> {
                Log.i(TAG, "Trying to decode as @DrawableRes")
                val bitmap = BitmapFactory.decodeResource(context.resources, image)
                postCanvasFrame(createCanvas(bitmap))
            }
            is Bitmap -> postCanvasFrame(createCanvas(image))
            is Canvas -> postCanvasFrame(image)
            else -> Log.e(TAG, "Image type $image is not supported. Try using a Canvas or a Bitmap")
        }
    }

    private fun createCanvas(bitmap: Bitmap): Canvas? {
        val canvas = getCanvas()
        canvas?.drawBitmap(bitmap, 0f, 0f, null)
        return canvas
    }

    /**
     *
     * @param canvas acquired from getCanvas()
     */
    private fun postCanvasFrame(canvas: Canvas?) {
        for (i in 0 until muxerConfig.framesPerImage) {
            surface.unlockCanvasAndPost(canvas)
            drainCodec(false)
        }
    }

    /**
     * Extracts all pending data from the encoder.
     *
     *
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     *
     * Borrows heavily from https://bigflake.com/mediacodec/EncodeAndMuxTest.java.txt
     */
    private fun drainCodec(endOfStream: Boolean) {
        if (VERBOSE) Log.d(TAG, "drainCodec($endOfStream)")
        if (endOfStream) {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder")
            mediaCodec.signalEndOfInputStream()
        }
        var encoderOutputBuffers: Array<ByteBuffer?>? = mediaCodec.getOutputBuffers()
        while (true) {
            val encoderStatus: Int = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC
                    .toLong())
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break // out of while
                } else {
                    if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS")
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mediaCodec.getOutputBuffers()
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (frameMuxer.isStarted()) {
                    throw RuntimeException("format changed twice")
                }
                val newFormat: MediaFormat = mediaCodec.outputFormat
                Log.d(TAG, "encoder output format changed: $newFormat")

                // now that we have the Magic Goodies, start the muxer
                frameMuxer.start(newFormat, audioExtractor)
            } else if (encoderStatus < 0) {
                Log.wtf(TAG, "unexpected result from encoder.dequeueOutputBuffer: $encoderStatus")
                // let's ignore it
            } else {
                val encodedData = encoderOutputBuffers?.get(encoderStatus)
                        ?: throw RuntimeException("encoderOutputBuffer  $encoderStatus was null")
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG")
                    bufferInfo.size = 0
                }
                if (bufferInfo.size != 0) {
                    if (!frameMuxer.isStarted()) {
                        throw RuntimeException("muxer hasn't started")
                    }
                    frameMuxer.muxVideoFrame(encodedData, bufferInfo)
                    if (VERBOSE) Log.d(TAG, "sent " + bufferInfo.size + " bytes to muxer")
                }
                mediaCodec.releaseOutputBuffer(encoderStatus, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly")
                    } else {
                        if (VERBOSE) Log.d(TAG, "end of stream reached")
                    }
                    break // out of while
                }
            }
        }
    }

    fun muxAudioFrames() {
        val sampleSize = 256 * 1024
        val offset = 100
        val audioBuffer = ByteBuffer.allocate(sampleSize)
        val audioBufferInfo = MediaCodec.BufferInfo()
        var sawEOS = false
        audioExtractor!!.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        var finalAudioTime: Long
        val finalVideoTime: Long = frameMuxer.getVideoTime()
        var audioTrackFrameCount = 0
        while (!sawEOS) {
            audioBufferInfo.offset = offset
            audioBufferInfo.size = audioExtractor!!.readSampleData(audioBuffer, offset)
            if (audioBufferInfo.size < 0) {
                if (VERBOSE) Log.d(TAG, "Saw input EOS.")
                audioBufferInfo.size = 0
                sawEOS = true
            } else {
                finalAudioTime = audioExtractor!!.sampleTime
                audioBufferInfo.presentationTimeUs = finalAudioTime
                audioBufferInfo.flags = audioExtractor!!.sampleFlags
                frameMuxer.muxAudioFrame(audioBuffer, audioBufferInfo)
                audioExtractor!!.advance()
                audioTrackFrameCount++
                if (VERBOSE) Log.d(TAG, "Frame ($audioTrackFrameCount Flags: ${audioBufferInfo.flags} Size(KB): ${audioBufferInfo.size / 1024}")
                // We want the sound to play for a few more seconds after the last image
                if ((finalAudioTime > finalVideoTime) &&
                        (finalAudioTime % finalVideoTime > muxerConfig.framesPerImage * SECOND_IN_USEC)) {
                    sawEOS = true
                    if (VERBOSE) Log.d(TAG, "Final audio time: $finalAudioTime video time: $finalVideoTime")
                }
            }
        }
    }

    /**
     * Releases encoder resources.  May be called after partial / failed initialization.
     */
    fun releaseVideoCodec() {
        // Release the video layer
        if (VERBOSE) Log.d(TAG, "releasing encoder objects")
        drainCodec(true)
        mediaCodec.stop()
        mediaCodec.release()
        surface.release()
    }

    fun releaseAudioExtractor() {
        audioExtractor?.release()
    }

    fun releaseMuxer() {
        // Release MediaMuxer
        frameMuxer.release()
    }

}