package com.webimageloader;

import java.io.File;
import java.util.EnumSet;

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
    public enum Flag {
        /**
         * Flag which makes the request ignore any possibly cached bitmaps
         */
        IGNORE_CACHE,
        /**
         * Flag which makes the request don't save its result to cache
         */
        NO_CACHE
    }

    private String url;
    private Transformation transformation;
    private EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);

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
     * @deprecated Use {@link #setTransformation(Transformation)} instead
     *
     * @param transformation the transformation to apply
     * @return the new request
     */
    @Deprecated
    public Request withTransformation(Transformation transformation) {
        return new Request(url, transformation);
    }

    /**
     * Set the transformation of this request
     *
     * @param transformation the transformation to apply
     * @return this request
     */
    public Request setTransformation(Transformation transformation) {
        this.transformation = transformation;

        return this;
    }

    /**
     * Add a flag to this request
     *
     * @param flag the flag to be added
     * @return this request
     */
    public Request addFlag(Flag flag) {
        flags.add(flag);

        return this;
    }

    /**
     * Add multiple flags to this request
     *
     * @param flags the flags to be added
     * @return this request
     */
    public Request addFlags(EnumSet<Flag> flags) {
        this.flags.addAll(flags);

        return this;
    }

    LoaderRequest toLoaderRequest() {
        return new LoaderRequest(url, transformation, flags);
    }

    private static String createUrl(String scheme, String authority, String path) {
        return scheme + "://" + authority + "/" + path;
    }
}
