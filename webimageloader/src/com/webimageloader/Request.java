package com.webimageloader;

import java.io.File;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.webimageloader.loader.LoaderRequest;
import com.webimageloader.transformation.Transformation;

public class Request {
    private String url;
    private Transformation transformation;

    public static Request forResource(Context c, int resId) {
        String url = createUrl(ContentResolver.SCHEME_ANDROID_RESOURCE, c.getPackageName(), String.valueOf(resId));
        return new Request(url);
    }

    public static Request forAsset(String path) {
        String url = createUrl(ContentResolver.SCHEME_FILE, "/android_asset/", path);
        return new Request(url);
    }

    public static Request forFile(File file) {
        String url = Uri.fromFile(file).toString();
        return new Request(url);
    }

    public Request(String url) {
        this.url = url;
    }

    public Request withTransformation(Transformation transformation) {
        this.transformation = transformation;

        return this;
    }

    LoaderRequest toLoaderRequest() {
        return new LoaderRequest(url, transformation);
    }

    private static String createUrl(String scheme, String authority, String path) {
        return scheme + "://" + authority + "/" + path;
    }
}
