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

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Volley request for multipart/form-data.
 */
public class MultipartRequest extends Request<NetworkResponse> {

    /**
     * Multipart data constants.
     */
    private final String BOUNDARY = "MultipartRequestBoundary-" + System.currentTimeMillis();
    private final String BODY_CONTENT_TYPE = "multipart/form-data;boundary=" + BOUNDARY;
    private final String LINE_END = "\r\n";
    private final String TWO_HYPHENS = "--";
    private final int MAX_BUFFER_SIZE_BYTES = 1024 * 1024; // 1 MiB.

    private Response.ErrorListener mErrorListener;
    private Response.Listener<NetworkResponse> mListener;
    private Map<String, String> mHeaders;
    private Map<String, DataPart> mMultipartData;

    /**
     * Create a multipart POST request with default headers and without multipart data.
     *
     * @param url
     * @param errorListener
     * @param listener
     */
    public MultipartRequest(String url,
                            Response.ErrorListener errorListener,
                            Response.Listener<NetworkResponse> listener) {
        this(Request.Method.POST, url, errorListener, listener, null, null);
    }

    /**
     * Create a multipart request.
     *
     * @param method {@link Request.Method}, e.g. POST, GET, etc.
     * @param url URL for request.
     * @param errorListener Error listener.
     * @param listener Response listener.
     * @param headers Request headers.
     * @param multipartData Multipart data.
     */
    public MultipartRequest(int method, String url,
                            Response.ErrorListener errorListener,
                            Response.Listener<NetworkResponse> listener,
                            Map<String, String> headers,
                            Map<String, DataPart> multipartData) {
        super(method, url, errorListener);
        mErrorListener = errorListener;
        mListener = listener;
        mHeaders = headers;
        if (multipartData == null) {
            multipartData = new HashMap<>();
        }
        mMultipartData = multipartData;
    }

    /**
     * Put data into multipart request.
     *
     * @param name Name of the multipart parameter.
     * @param dataPart Multipart data bytes.
     */
    public void putDataPart(String name, DataPart dataPart) {
        mMultipartData.put(name, dataPart);
    }

    /**
     * @return Headers, or default headers if headers are null.
     * @throws AuthFailureError
     */
    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return (mHeaders != null) ? mHeaders : super.getHeaders();
    }

    @Override
    public String getBodyContentType() {
        return BODY_CONTENT_TYPE;
    }

    /**
     * @return bytes for the multipart/form-data request.
     * @throws AuthFailureError
     */
    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            // populate text payload
            Map<String, String> params = getParams();
            if (params != null && params.size() > 0) {
                buildTextSection(dos, params, getParamsEncoding());
            }

            // populate data byte payload
            Map<String, DataPart> data = getMultipartData();
            if (data != null && data.size() > 0) {
                buildMultipartSection(dos, data);
            }

            // close multipart form data after text and file data
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END);

            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Custom method handle data payload.
     *
     * @return Map data part label with data byte
     * @throws AuthFailureError
     */
    protected Map<String, DataPart> getMultipartData() throws AuthFailureError {
        return mMultipartData;
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        try {
            return Response.success(
                    response,
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        mErrorListener.onErrorResponse(error);
    }

    /**
     * Build text section of request.
     *
     * @param dataOutputStream Data output.
     * @param params Text parameter names and values.
     * @param encoding If null or empty, encoding defaults to "UTF-8".
     * @throws IOException
     */
    private void buildTextSection(DataOutputStream dataOutputStream, Map<String, String> params,
                                  String encoding) throws IOException {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            buildTextPart(dataOutputStream, entry.getKey(), entry.getValue(), encoding);
        }
    }

    /**
     * Write text parameter bytes into {@param dataOutputStream}.
     *
     * @param dataOutputStream Data output.
     * @param parameterName Text parameter name.
     * @param parameterValue Text parameter value.
     * @param encoding If null or empty, encoding defaults to "UTF-8".
     * @throws IOException
     */
    private void buildTextPart(DataOutputStream dataOutputStream, String parameterName,
                               String parameterValue, String encoding) throws IOException {
        dataOutputStream.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_END);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\""
                + parameterName + "\"" + LINE_END);
        if (encoding != null && !encoding.trim().isEmpty()) {
            dataOutputStream.writeBytes("Content-Type: text/plain; charset=" + encoding + LINE_END);
        } else {
            dataOutputStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + LINE_END);
        }
        dataOutputStream.writeBytes(LINE_END);
        dataOutputStream.writeBytes(parameterValue + LINE_END);
    }

    /**
     * Build multipart section of request.
     *
     * @param dataOutputStream Data output.
     * @param data Multipart parameter names and values.
     * @throws IOException
     */
    private void buildMultipartSection(DataOutputStream dataOutputStream,
                                       Map<String, DataPart> data) throws IOException {
        for (Map.Entry<String, DataPart> entry : data.entrySet()) {
            buildMultidataPart(dataOutputStream, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Write multipart data into {@param dataOutputStream}.
     *
     * @param dataOutputStream Data output.
     * @param parameterName Multipart parameter name.
     * @param parameterValue Multipart parameter value.
     * @throws IOException
     */
    private void buildMultidataPart(DataOutputStream dataOutputStream, String parameterName,
                                    DataPart parameterValue) throws IOException {
        dataOutputStream.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_END);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" +
                parameterName + "\"; filename=\"" + parameterValue.getFilename() + "\"" + LINE_END);
        if (parameterValue.getMimeType() != null
                && !parameterValue.getMimeType().trim().isEmpty()) {
            dataOutputStream.writeBytes("Content-Type: " + parameterValue.getMimeType() + LINE_END);
        }
        dataOutputStream.writeBytes(LINE_END);

        ByteArrayInputStream fileInputStream = new ByteArrayInputStream(parameterValue.getData());
        int bytesAvailable = fileInputStream.available();
        int bufferSize = Math.min(bytesAvailable, MAX_BUFFER_SIZE_BYTES);
        byte[] buffer = new byte[bufferSize];

        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        while (bytesRead > 0) {
            dataOutputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, MAX_BUFFER_SIZE_BYTES);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }
        dataOutputStream.writeBytes(LINE_END);
    }
}