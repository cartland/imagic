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

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.integration.volley.VolleyUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.GlideModule;

import java.io.File;
import java.io.InputStream;

/**
 * Custom {@link GlideModule} in order to increase request timeout.
 */
public class CustomGlide implements GlideModule {

    private static final int TIMEOUT_MS = 10000;

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        RequestQueue queue = new RequestQueue( // params hardcoded from Volley.newRequestQueue()
                new DiskBasedCache(new File(context.getCacheDir(), "volley")),
                new BasicNetwork(new HurlStack())) {
            @Override
            public <T> Request<T> add(Request<T> request) {
                request.setRetryPolicy(new DefaultRetryPolicy(TIMEOUT_MS, 1, 1));
                return super.add(request);
            }
        };
        queue.start();
        glide.register(GlideUrl.class, InputStream.class, new VolleyUrlLoader.Factory(queue));
    }
}