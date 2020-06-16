package com.homesoft.bitmap2video;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;

import static com.homesoft.bitmap2video.FileUtils.getFileDescriptor;
import static com.homesoft.bitmap2video.FileUtils.getVideoFile;
import static com.homesoft.bitmap2video.FileUtils.shareVideo;

/*
 * Copyright (C) 2019 Homesoft, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int BITRATE_DEFAULT = 1500000;
    private static final int DEFAULT_WIDTH = 320;
    private static final int DEFAULT_HEIGHT = 240;
    private static final int FPS = 1;
    private static final int FRAMES_PER_IMAGE = 1;

    private VideoView mVideoPlayer;
    private CreateRunnable mCreateRunnable;
    private Button mPlay;
    private Button mShare;
    private RadioGroup mCodec;
    private RadioButton mAvc, mHevc;
    private File videoFile;
    private EncoderConfig encoderConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        createActionBarBorder(toolbar);
        findViewById(R.id.make).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                encoderConfig = setupEncoder();
                if (encoderConfig != null) {
                    AsyncTask.THREAD_POOL_EXECUTOR.execute(mCreateRunnable =
                            new CreateRunnable(MainActivity.this, encoderConfig, true));
                } else {
                    Log.e(TAG, "Encoder config is null!");
                }
            }
        });
        mVideoPlayer = findViewById(R.id.player);
        mShare = findViewById(R.id.share);
        mShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareVideo(MainActivity.this, videoFile, encoderConfig.getMimeType());
            }
        });
        mPlay = findViewById(R.id.play);
        mPlay.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                mVideoPlayer.setVideoPath(videoFile.getAbsolutePath());
                mVideoPlayer.start();
            }
        });
        mCodec = findViewById(R.id.codec);
        mAvc = findViewById(R.id.avc);
        mHevc = findViewById(R.id.hevc);
        mAvc.setEnabled(EncoderConfig.isSupported(AvcEncoderConfig.MIME_TYPE));
        mHevc.setEnabled(EncoderConfig.isSupported(HevcEncoderConfig.MIME_TYPE));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1234);
        }
    }

    private EncoderConfig setupEncoder() {
        final EncoderConfig encoderConfig;
        final int radioId = mCodec.getCheckedRadioButtonId();
        if (radioId == mAvc.getId()) {
            encoderConfig = new AvcEncoderConfig(FPS, BITRATE_DEFAULT);
        } else if (radioId == mHevc.getId()) {
            encoderConfig = new HevcEncoderConfig(FPS, BITRATE_DEFAULT);
        } else {
            return null;
        }

        videoFile = getVideoFile(MainActivity.this, "test.mp4");
        encoderConfig.setPath(videoFile.getAbsolutePath());
        encoderConfig.setAudioTrackFileDescriptor(getFileDescriptor(MainActivity.this,
                R.raw.bensound_happyrock));
        encoderConfig.setFramesPerImage(FRAMES_PER_IMAGE);
        encoderConfig.setHeight(DEFAULT_HEIGHT);
        encoderConfig.setWidth(DEFAULT_WIDTH);
        return encoderConfig;
    }

    private static float getFloat(final Resources res, int id) {
        TypedValue typedValue = new TypedValue();
        res.getValue(id, typedValue, true);
        return typedValue.getFloat();
    }


    void done() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPlay.setEnabled(true);
                mShare.setEnabled(true);
            }
        });
    }
}
