package com.webimageloader.loader;

import com.webimageloader.transformation.Transformation;

public class LoaderRequest {
    private String url;
    private Transformation transformation;
    private Metadata metadata;

    private String cacheKey = null;

    public LoaderRequest(String url) {
        this(url, null);
    }

    public LoaderRequest(String url, Transformation transformation) {
        if (url == null) {
            throw new IllegalArgumentException("url may not be null");
        }

        this.url = url;
        this.transformation = transformation;
    }

    public LoaderRequest withoutTransformation() {
        return new LoaderRequest(url);
    }

    public LoaderRequest withMetadata(Metadata metadata) {
        LoaderRequest r = new LoaderRequest(url, transformation);
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

    public synchronized String getCacheKey() {
        if (cacheKey == null) {
            if (transformation != null) {
                cacheKey = url + transformation.getIdentifier();
            } else {
                cacheKey = url;
            }
        }

        return cacheKey;
    }

    @Override
    public int hashCode() {
        return getCacheKey().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LoaderRequest) {
            LoaderRequest request = (LoaderRequest) obj;
            return getCacheKey().equals(request.getCacheKey());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        if (transformation != null) {
            return url + " with transformation " + '"' + transformation.getIdentifier() + '"';
        } else {
            return url;
        }
    }
}
