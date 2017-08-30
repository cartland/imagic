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

package com.chriscartland.imagic;

import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.chriscartland.imagic.network.DataPart;
import com.chriscartland.imagic.network.MultipartRequest;
import com.chriscartland.imagic.network.VolleySingleton;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String IMAGE_URL_STATE = "image_state";
    private static final String DEPTH_URL_STATE = "depth_state";
    private static final String BACKGROUND_URL_STATE = "background_state";
    private static final int TIMEOUT_MS = 10000;
    private MainActivity mContext;
    private String mUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Loading image", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                String depthUrl = ((EditText) findViewById(R.id.depth_url)).getText().toString();
                String backgroundUrl =
                        ((EditText) findViewById(R.id.background_url)).getText().toString();

                ImagicUrl.Builder urlBuilder = new ImagicUrl.Builder();
                urlBuilder.setScheme(getString(R.string.scheme));
                urlBuilder.setHost(getString(R.string.host));
                urlBuilder.setPath(getString(R.string.path));
                urlBuilder.addParam(getString(R.string.depth), depthUrl);
                urlBuilder.addParam(getString(R.string.background), backgroundUrl);
                mUrl = urlBuilder.build().toString();
                processImages();
            }
        });
    }

    /**
     * Create image based on URLs.
     */
    private void updateImage() {
        ImageView imageView = (ImageView) findViewById(R.id.output_image);
        Glide.with(mContext).load(mUrl)
                .placeholder(getProgressBarIndeterminate()).into(imageView);
    }

    /**
     * Creage image based on uploaded images.
     */
    private void processImages() {
        // TODO(cartland): Move this work off the UI thread.

        final ImageView imageView = (ImageView) findViewById(R.id.output_image);
        Response.Listener<NetworkResponse> listener = new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                byte[] data = response.data;
                Log.d(TAG, "Image processed. Displaying result.");
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                imageView.setImageBitmap(bmp);
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                String errorMessage = "Unknown error";
                if (networkResponse == null) {
                    if (error.getClass().equals(TimeoutError.class)) {
                        errorMessage = "Request timeout";
                    } else if (error.getClass().equals(NoConnectionError.class)) {
                        errorMessage = "Failed to connect server";
                    }
                } else {
                    String result = new String(networkResponse.data);
                    Log.e(TAG, result);
                }
                error.printStackTrace();
            }
        };

        MultipartRequest multipartRequest = new MultipartRequest(mUrl, errorListener, listener);
        DataPart background = new DataPart("chefchaouen.jpg",
                DataPart.getFileDataFromDrawable(getDrawable(R.drawable.chefchaouen)),
                "image/jpeg");
        multipartRequest.putDataPart("background", background);

        DataPart depth = new DataPart("borrodepth.png",
                DataPart.getFileDataFromDrawable(getDrawable(R.drawable.borrodepth)),
                "image/jpeg");
        multipartRequest.putDataPart("depth", depth);

        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS, 1, 1));
        VolleySingleton.getInstance(getBaseContext()).addToRequestQueue(multipartRequest);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mUrl = savedInstanceState.getString(IMAGE_URL_STATE);
        if (mUrl != null) {
            updateImage();
        }
        String depthUrl = savedInstanceState.getString(DEPTH_URL_STATE);
        ((EditText)findViewById(R.id.depth_url)).setText(depthUrl);
        String backgroundUrl = savedInstanceState.getString(BACKGROUND_URL_STATE);
        ((EditText)findViewById(R.id.background_url)).setText(backgroundUrl);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(IMAGE_URL_STATE, mUrl);
        outState.putString(DEPTH_URL_STATE,
                ((EditText)findViewById(R.id.depth_url)).getText().toString());
        outState.putString(BACKGROUND_URL_STATE,
                ((EditText)findViewById(R.id.background_url)).getText().toString());
    }

    private Drawable getProgressBarIndeterminate() {
        final int[] attrs = {android.R.attr.indeterminateDrawable};
        final int attrs_indeterminateDrawable_index = 0;
        TypedArray a = mContext.obtainStyledAttributes(android.R.style.Widget_ProgressBar, attrs);
        try {
            return a.getDrawable(attrs_indeterminateDrawable_index);
        } finally {
            a.recycle();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
