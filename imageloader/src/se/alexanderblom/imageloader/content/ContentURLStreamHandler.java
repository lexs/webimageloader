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

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import android.content.ContentResolver;

/**
 * {@link URLStreamHandler} for {@code content://}, {@code file://}, and {@code
 * android.resource://} URIs.
 */
public class ContentURLStreamHandler extends URLStreamHandler {

    private final ContentResolver mResolver;

    public ContentURLStreamHandler(ContentResolver resolver) {
        if (resolver == null) {
            throw new NullPointerException();
        }
        mResolver = resolver;
    }

    @Override
    protected URLConnection openConnection(URL url) {
        return new ContentURLConnection(mResolver, url);
    }
}