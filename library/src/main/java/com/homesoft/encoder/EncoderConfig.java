package com.homesoft.encoder;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;

import java.io.IOException;

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

public abstract class EncoderConfig {

    private final float framesPerSecond;
    private final int bitRate;

    private String path;
    private int width;
    private int height;
    private String mimeType;
    private int audioTrack;
    private int framesPerImage;

    abstract FrameMuxer getFrameMuxer() throws IOException;
    abstract MediaFormat getVideoMediaFormat();

    public static boolean isSupported(final String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public EncoderConfig(final float framesPerSecond, final int bitRate) {
        this.framesPerSecond = framesPerSecond;
        this.bitRate = bitRate;
    }

    public EncoderConfig(final String path, final int width, final int height, final float framesPerSecond, final int bitRate) {
        this.path = path;
        this.width = width;
        this.height = height;
        this.framesPerSecond = framesPerSecond;
        this.bitRate = bitRate;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBitRate() {
        return bitRate;
    }

    public float getFramePerSecond() {
        return framesPerSecond;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public int getAudioTrack() {
        return audioTrack;
    }

    public void setAudioTrack(int track) {
        this.audioTrack = audioTracks[track];
    }

    public int getFramesPerImage() {
        return framesPerImage;
    }

    public void setFramesPerImage(int framesPerImage) {
        this.framesPerImage = framesPerImage;
    }
}
