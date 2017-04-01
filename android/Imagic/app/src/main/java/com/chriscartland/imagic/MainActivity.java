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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String IMAGE_URL_STATE = "image_state";
    private static final String DEPTH_URL_STATE = "depth_state";
    private static final String BACKGROUND_URL_STATE = "background_state";
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
                updateImage();
            }
        });
    }

    private void updateImage() {
        ImageView imageView = (ImageView) findViewById(R.id.output_image);
        Glide.with(mContext).load(mUrl)
                .placeholder(getProgressBarIndeterminate()).into(imageView);
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
