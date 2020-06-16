package com.homesoft.bitmap2video;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.core.content.FileProvider;

import java.io.File;

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

public class FileUtils {

    private static final String TAG = FileUtils.class.getSimpleName();

    private static final String MEDIA_FILE_PATH = "media";
    private static final String FILE_AUTHORITY = "com.homesoft.fileprovider";

    /**
     * Get a {@link AssetFileDescriptor} from a raw resource
     *
     * @param context - activity or application context
     * @param rawAudioResource - resource from {@code R.raw}
     * @return - AssetFileDescriptor from resource
     */
    public static AssetFileDescriptor getFileDescriptor(@NonNull final Context context,
                                                        @RawRes int rawAudioResource) {
        return context.getResources().openRawResourceFd(rawAudioResource);
    }

    /**
     * Get a File object where we will be storing the video
     *
     * @param context - Activity or application context
     * @param fileName - name of the file
     * @return - created file object at media/fileName
     */
    public static File getVideoFile(@NonNull final Context context, final String fileName) {
        return getVideoFile(context, MEDIA_FILE_PATH, fileName);
    }

    /**
     * Get a File object where we will be storing the video
     *
     * @param context - Activity or application context
     * @param fileDir - name of directory where video file is stored
     *                IMPORTANT: if setting this, you must provide the appropriate {@code paths}
     *                in your xml resources {@see file_paths.xml}, as well as set up your
     *                provider with the appropriate file paths.
     * @param fileName - name of the file
     * @return - created file object at fileDir/fileName
     */
    public static File getVideoFile(@NonNull final Context context, @NonNull final String fileDir,
                                    @NonNull final String fileName) {
        final File mediaFolder = new File(context.getFilesDir(), fileDir);
        // Create the directory if it does not exist
        if (!mediaFolder.exists()) mediaFolder.mkdirs();
        Log.d(TAG, "Got folder at: " + mediaFolder.getAbsolutePath());
        final File file = new File(mediaFolder, fileName);
        Log.d(TAG, "Got file at: " + file.getAbsolutePath());
        return file;
    }

    /**
     * Creates an implicit intent to share the video file with any apps that accept it
     *
     * @param context - Activity or application context
     * @param file - File object (where video was saved)
     * @param mimeType - mime type of video as a string, can be retrieved from encoder
     * @return true if sharing was successful, false otherwise
     */
    public static boolean shareVideo(@NonNull final Context context, @NonNull final File file,
                                     @NonNull final String mimeType) {
        return shareVideo(context, file, mimeType, FILE_AUTHORITY);
    }

    /**
     * Creates an implicit intent to share the video file with any apps that accept it
     *
     * @param context - Activity or application context
     * @param file - File object (where video was saved)
     * @param mimeType - mime type of video as a string, can be retrieved from encoder
     * @param fileAuthority - Application file authority
     * @return true if sharing was successful, false otherwise
     */
    public static boolean shareVideo(@NonNull final Context context, @NonNull final File file,
                                  @NonNull final String mimeType, @NonNull String fileAuthority) {
        if (!file.exists()) {
            return false;
        }
        Log.d(TAG, "Found file at " + file.getAbsolutePath());
        Uri uri = FileProvider.getUriForFile(context, fileAuthority, file);
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType(mimeType);
        context.startActivity(intent);
        return true;
    }
}
