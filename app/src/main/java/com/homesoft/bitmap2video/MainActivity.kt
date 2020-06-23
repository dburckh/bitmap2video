package com.homesoft.bitmap2video

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import com.homesoft.bitmap2video.FileUtils.getVideoFile
import com.homesoft.encoder.Muxer
import com.homesoft.encoder.MuxingCompletionListener
import com.homesoft.encoder.isCodecSupported
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    companion object {
        val TAG = MainActivity::class.java.simpleName
        val imageArray: List<Int> = listOf(
                R.raw.im1,
                R.raw.im2,
                R.raw.im3,
                R.raw.im4
        )
    }

    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Basic implementation
        bt_make.setOnClickListener {
            bt_make.isEnabled = false
            val videoFile = getVideoFile(this@MainActivity, "test.mp4")

            val muxer: Muxer = Muxer(this@MainActivity, videoFile.absolutePath)
            scope.launch {
                muxer.setOnMuxingCompleted(object : MuxingCompletionListener {
                    override fun onVideoSuccessful(filepath: String) {
                        Log.d(TAG, "Video muxed - file path: $filepath")
                    }

                    override fun onVideoError(error: Throwable) {
                        Log.e(TAG, "There was an error muxing the video")
                    }

                })

                muxer.mux(imageArray, R.raw.bensound_happyrock)
            }
        }

        avc.isEnabled = isCodecSupported(MediaFormat.MIMETYPE_VIDEO_AVC)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1234)
        }
    }
}
