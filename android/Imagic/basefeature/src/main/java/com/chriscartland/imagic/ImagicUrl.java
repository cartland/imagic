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

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ImagicUrl {
    private static final String TAG = ImagicUrl.class.getCanonicalName();

    private String mUrl;

    @Override
    public String toString() {
        return mUrl;
    }

    public static class Builder {

        private String mScheme;
        private String mHost;
        private String mPath;
        private Map<String, ArrayList<String>> mParams;

        public Builder() {
            mScheme = null;
            mHost = null;
            mPath = "";
            mParams = new HashMap<>();
        }

        public void setScheme(String scheme) {
            mScheme = scheme;
        }

        public void setHost(String host) {
            mHost = host;
        }

        public void setPath(String path) {
            mPath = path;
        }

        /**
         * Add a URL parameter.
         *
         * @param name Parameter name will be URL encoded.
         * @param newValue Parameter value will be URL encoded.
         */
        public void addParam(String name, String newValue) {
            try {
                String urlEncodedKey = URLEncoder.encode(name, "utf-8");
                String urlEncodedValue = URLEncoder.encode(newValue, "utf-8");
                ArrayList<String> values;
                if (mParams.containsKey(urlEncodedKey)) {
                    values = mParams.get(urlEncodedKey);
                } else {
                    values = new ArrayList<>();
                }
                values.add(urlEncodedValue);
                mParams.put(urlEncodedKey, values);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, e.toString());
            }
        }

        /**
         * Build the ImagicUrl. Scheme and host must be set.
         *
         * @return Fully formed {@link ImagicUrl}.
         * @throws IllegalStateException Throws exception if scheme or host is not set.
         */
        public ImagicUrl build() throws IllegalStateException {
            ImagicUrl result = new ImagicUrl();
            result.mUrl = buildString();
            return result;
        }

        private String buildString() throws IllegalStateException {
            StringBuilder sb = new StringBuilder();
            if (mScheme == null) {
                throw new IllegalStateException("Scheme must be set");
            }
            if (mHost == null) {
                throw new IllegalStateException("Host must be set");
            }
            sb.append(mScheme);
            sb.append(mHost);
            sb.append(mPath);
            int paramCount = 0;
            Iterator<Map.Entry<String, ArrayList<String>>> it = mParams.entrySet().iterator();
            while (it.hasNext()) {
                if (paramCount == 0) {
                    sb.append("?");
                } else {
                    sb.append("&");
                }
                Map.Entry<String, ArrayList<String>> pair = it.next();
                String name = pair.getKey();
                for (String value : pair.getValue()) {
                    sb.append(name);
                    sb.append("=");
                    sb.append(value);
                }
                it.remove(); // avoids a ConcurrentModificationException
                paramCount++;
            }
            return sb.toString();
        }
    }
}
