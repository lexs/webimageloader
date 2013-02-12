package com.webimageloader.loader;

import com.webimageloader.Request;
import com.webimageloader.transformation.Transformation;

import java.util.EnumSet;

public class LoaderRequest {
    private String url;
    private Transformation transformation;
    private Metadata metadata;
    private EnumSet<Request.Flag> flags;

    private String cacheKey;

    public LoaderRequest(String url, Transformation transformation, EnumSet<Request.Flag> flags) {
        if (url == null) {
            throw new IllegalArgumentException("url may not be null");
        }

        this.url = url;
        this.transformation = transformation;
        this.flags = flags;

        if (transformation != null) {
            cacheKey = url + transformation.getIdentifier();
        } else {
            cacheKey = url;
        }
    }

    public LoaderRequest withoutTransformation() {
        return new LoaderRequest(url, null, flags);
    }

    public LoaderRequest withMetadata(Metadata metadata) {
        LoaderRequest r = new LoaderRequest(url, transformation, flags);
        r.metadata = metadata;

        return r;
    }

    public String getUrl() {
        return url;
    }

    public Transformation getTransformation() {
        return transformation;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public boolean hasFlag(Request.Flag flag) {
        return flags.contains(flag);
    }

    @Override
    public int hashCode() {
        return cacheKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LoaderRequest) {
            LoaderRequest request = (LoaderRequest) obj;
            return cacheKey.equals(request.getCacheKey());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        String f = flags.isEmpty() ? "" : ", flags=" + flags;

        if (transformation != null) {
            return url + f + " with transformation " + '"' + transformation.getIdentifier() + '"';
        } else {
            return url + f;
        }
    }
}
