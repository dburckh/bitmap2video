import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.homesoft.bitmap2video.CreateRunnable
import com.homesoft.bitmap2video.FileUtils
import com.homesoft.bitmap2video.MainActivity
import com.homesoft.bitmap2video.R
import com.homesoft.encoder.MuxerConfig
import java.io.File

class MainActivity: AppCompatActivity() {

    val TAG = MainActivity::class.java.simpleName

    val BITRATE_DEFAULT = 1500000
    val DEFAULT_WIDTH = 320
    val DEFAULT_HEIGHT = 240
    val FPS = 1
    val FRAMES_PER_IMAGE = 1

    var mVideoPlayer: VideoView? = null
    var mCreateRunnable: CreateRunnable? = null
    var mPlay: Button? = null
    var mShare: Button? = null
    var mCodec: RadioGroup? = null
    var mAvc: RadioButton? = null
    private  var mHevc:RadioButton? = null
    var videoFile: File? = null
    var muxerConfig: MuxerConfig? = null

    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        make.setOnClickListener(View.OnClickListener {
            muxerConfig = setupEncoder()
            if (muxerConfig != null) {
                AsyncTask.THREAD_POOL_EXECUTOR.execute(CreateRunnable(this@MainActivity, muxerConfig, true).also({ mCreateRunnable = it }))
            } else {
                Log.e(TAG, "Encoder config is null!")
            }
        })

        mShare!!.setOnClickListener { FileUtils.shareVideo(this@MainActivity, videoFile!!,
                muxerConfig.mimeType) }
        mPlay!!.setOnClickListener {
            mVideoPlayer!!.setVideoPath(videoFile!!.absolutePath)
            mVideoPlayer!!.start()
        }

        mAvc!!.isEnabled = EncoderConfig.isSupported(AvcEncoderConfig.MIME_TYPE)
        mHevc.setEnabled(EncoderConfig.isSupported(HevcEncoderConfig.MIME_TYPE))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1234)
        }
    }

    private fun setupEncoder(): EncoderConfig? {
        val encoderConfig: EncoderConfig
        val radioId = mCodec!!.checkedRadioButtonId
        if (radioId == mAvc!!.id) {
            encoderConfig = AvcEncoderConfig(MainActivity.FPS, MainActivity.BITRATE_DEFAULT)
        } else if (radioId == mHevc.getId()) {
            encoderConfig = HevcEncoderConfig(MainActivity.FPS, MainActivity.BITRATE_DEFAULT)
        } else {
            return null
        }
        videoFile = FileUtils.getVideoFile(this@MainActivity, "test.mp4")
        encoderConfig.setPath(videoFile.getAbsolutePath())
        encoderConfig.setAudioTrackFileDescriptor(FileUtils.getFileDescriptor(this@MainActivity,
                R.raw.bensound_happyrock))
        encoderConfig.setFramesPerImage(MainActivity.FRAMES_PER_IMAGE)
        encoderConfig.setHeight(MainActivity.DEFAULT_HEIGHT)
        encoderConfig.setWidth(MainActivity.DEFAULT_WIDTH)
        return encoderConfig
    }

    private fun getFloat(res: Resources, id: Int): Float {
        val typedValue = TypedValue()
        res.getValue(id, typedValue, true)
        return typedValue.float
    }


    fun done() {
        runOnUiThread {
            mPlay!!.isEnabled = true
            mShare!!.isEnabled = true
        }
    }

}
