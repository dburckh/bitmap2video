//package com.homesoft.bitmap2video
//
//class CreateRunnable internal constructor(private val mMainActivity: MainActivity, encoderConfig: EncoderConfig, addText: Boolean) : Runnable {
//    private val mEncoderConfig: EncoderConfig
//    private var mPaint: Paint? = null
//    var outputPath: String? = null
//        private set
//
//    override fun run() {
//        val frameEncoder = FrameEncoder(mEncoderConfig)
//        try {
//            frameEncoder.start()
//        } catch (e: IOException) {
//            Log.e(TAG, "Start Encoder Failed", e)
//            return
//        }
//        mPaint.setTextSize(mEncoderConfig.getHeight() / 2)
//        val resources: Resources = mMainActivity.resources
//        for (i in 0 until FRAMES) {
//            val bitmap: Bitmap = BitmapFactory.decodeStream(resources.openRawResource(IMAGE_IDS[i and 3]))
//            if (mPaint == null) {
//                frameEncoder.createFrame(bitmap)
//            } else {
//                val canvas: Canvas = frameEncoder.getCanvas()
//                canvas.drawBitmap(bitmap, 0f, 0f, null)
//                val text = Character.toString(('A'.toInt() + i).toChar())
//                canvas.drawText(text, 0, mEncoderConfig.getHeight(), mPaint)
//                frameEncoder.createFrame(canvas)
//            }
//        }
//        frameEncoder.releaseVideoEncoder()
//        if (mEncoderConfig.getAudioTrackFileDescriptor() != null) {
//            // Mux in the audio after we release the video encoder
//            frameEncoder.muxAudioFrames()
//        }
//        frameEncoder.releaseMuxer()
//        outputPath = mEncoderConfig.getPath()
//        mMainActivity.done()
//    }
//
//    companion object {
//        private val IMAGE_IDS = intArrayOf(R.raw.im1, R.raw.im2, R.raw.im3, R.raw.im4)
//        private const val FRAMES = 30
//        private val TAG = CreateRunnable::class.java.simpleName
//    }
//
//    init {
//        mEncoderConfig = encoderConfig
//        if (addText) {
//            mPaint = Paint()
//            mPaint.setColor(Color.WHITE)
//        } else {
//            mPaint = null
//        }
//    }
//}
