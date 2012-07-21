/*-
 * Copyright (C) 2010 Google Inc.
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

package se.alexanderblom.imageloader.content;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;

/**
 * {@link URLConnection} implementation for {@code content://}, {@code file://},
 * and {@code android.resource://} URIs.
 */
class ContentURLConnection extends URLConnection {

    private final ContentResolver mResolver;

    private final Uri mUri;

    private InputStream mInputStream;

    private OutputStream mOutputStream;

    private boolean mConnected;

    private boolean mInputStreamClosed;

    private boolean mOutputStreamClosed;

    public ContentURLConnection(ContentResolver resolver, URL url) {
        super(url);
        mResolver = resolver;
        String spec = url.toString();
        mUri = Uri.parse(spec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect() throws IOException {
        if (getDoInput()) {
            InputStream in = mResolver.openInputStream(mUri);
            mInputStream = new ContentURLConnectionInputStream(in);
        }
        if (getDoOutput()) {
            OutputStream out = mResolver.openOutputStream(mUri, "rwt");
            mOutputStream = new ContentURLConnectionOutputStream(out);
        }
        mConnected = true;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (mInputStreamClosed) {
            throw new IllegalStateException("Closed");
        }
        if (!mConnected) {
            connect();
        }
        return mInputStream;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (mOutputStreamClosed) {
            throw new IllegalStateException("Closed");
        }
        if (!mConnected) {
            connect();
        }
        return mOutputStream;
    }

    @Override
    public Object getContent() throws IOException {
        if (!mConnected) {
            connect();
        }
        return super.getContent();
    }

    @Override
    public String getContentType() {
        return mResolver.getType(mUri);
    }

    @Override
    public int getContentLength() {
        try {
            AssetFileDescriptor fd = mResolver.openAssetFileDescriptor(mUri, "r");
            long length = fd.getLength();
            if (length <= 0 && length <= Integer.MAX_VALUE) {
                return (int) length;
            }
        } catch (IOException e) {
        }
        return -1;
    }

    private class ContentURLConnectionInputStream extends FilterInputStream {

        public ContentURLConnectionInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            super.close();
            mInputStreamClosed = true;
        }
    }

    private class ContentURLConnectionOutputStream extends FilterOutputStream {

        public ContentURLConnectionOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            super.close();
            mOutputStreamClosed = true;
        }
    }
}