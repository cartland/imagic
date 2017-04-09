/*
 * Copyright 2017 Chris Cartland. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.chriscartland.imagic.network;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.ByteArrayOutputStream;

/**
 * File data for a multipart/form-data HTTP request.
 */
public class DataPart {

    private String mFilename;
    private byte[] mData;
    private String mMimeType;

    /**
     * Byte data for a {@link Drawable}.
     *
     * @param drawable {@link Drawable} resource.
     * @return PNG byte array.
     */
    public static byte[] getFileDataFromDrawable(Drawable drawable) {
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Constructor with mime data mMimeType.
     *
     * @param name     label of data
     * @param data     byte data
     * @param mimeType mime data like "image/jpeg"
     */
    public DataPart(String name, byte[] data, String mimeType) {
        mFilename = name;
        mData = data;
        mMimeType = mimeType;
    }

    /**
     * Getter file name.
     *
     * @return file name
     */
    public String getFilename() {
        return mFilename;
    }

    /**
     * Setter file name.
     *
     * @param fileName string file name
     */
    public void setFilename(String fileName) {
        mFilename = fileName;
    }

    /**
     * Getter mData.
     *
     * @return byte file data
     */
    public byte[] getData() {
        return mData;
    }

    /**
     * Setter mData.
     *
     * @param data byte file data
     */
    public void setData(byte[] data) {
        mData = data;
    }

    /**
     * Getter mime mMimeType.
     *
     * @return mime mMimeType
     */
    public String getMimeType() {
        return mMimeType;
    }

    /**
     * Setter mime mMimeType.
     *
     * @param type mime mMimeType
     */
    public void setMimeType(String type) {
        mMimeType = type;
    }
}