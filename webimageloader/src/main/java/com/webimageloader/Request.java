package com.webimageloader;

import java.io.File;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.webimageloader.loader.LoaderRequest;
import com.webimageloader.transformation.Transformation;

/**
 * Class describing a specific request including transformations to be applied.
 *
 * @author Alexander Blom <alexanderblom.se>
 */
public class Request {
    private String url;
    private Transformation transformation;

    /**
     * Create a request for a resource in /res
     *
     * @param c the context to use
     * @param resId the resource if
     * @return a request for this resource
     */
    public static Request forResource(Context c, int resId) {
        String url = createUrl(ContentResolver.SCHEME_ANDROID_RESOURCE, c.getPackageName(), String.valueOf(resId));
        return new Request(url);
    }

    /**
     * Create a request for an asset in /assets
     *
     * @param path the path of this asset
     * @return a request for this asset
     */
    public static Request forAsset(String path) {
        String url = createUrl(ContentResolver.SCHEME_FILE, "/android_asset/", path);
        return new Request(url);
    }

    /**
     * Create a request for this file on the local file system
     *
     * @param file path to the file
     * @return a request for this file
     */
    public static Request forFile(File file) {
        String url = Uri.fromFile(file).toString();
        return new Request(url);
    }

    /**
     * Constructor for a specific url
     *
     * @param url the url
     */
    public Request(String url) {
        this.url = url;
    }

    /**
     * Constructor for a specific url and transformation
     *
     * @param url the url
     * @param transformation the transformation
     */
    public Request(String url, Transformation transformation) {
        this.url = url;
        this.transformation = transformation;
    }

    public String getUrl() {
        return url;
    }

    public Transformation getTransformation() {
        return transformation;
    }

    /**
     * Create a new request with an added transformation
     *
     * @param transformation the transformation to apply
     * @return the new request
     */
    public Request withTransformation(Transformation transformation) {
        return new Request(url, transformation);
    }

    LoaderRequest toLoaderRequest() {
        return new LoaderRequest(url, transformation);
    }

    private static String createUrl(String scheme, String authority, String path) {
        return scheme + "://" + authority + "/" + path;
    }
}
