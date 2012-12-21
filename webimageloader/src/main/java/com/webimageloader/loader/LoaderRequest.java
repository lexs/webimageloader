package com.webimageloader.loader;

import com.webimageloader.transformation.Transformation;

public class LoaderRequest {
    private String url;
    private Transformation transformation;
    private Metadata metadata;

    private String cacheKey;

    public LoaderRequest(String url) {
        this(url, null);
    }

    public LoaderRequest(String url, Transformation transformation) {
        if (url == null) {
            throw new IllegalArgumentException("url may not be null");
        }

        this.url = url;
        this.transformation = transformation;

        if (transformation != null) {
            cacheKey = url + transformation.getIdentifier();
        } else {
            cacheKey = url;
        }
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

    public String getCacheKey() {
        return cacheKey;
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
        if (transformation != null) {
            return url + " with transformation " + '"' + transformation.getIdentifier() + '"';
        } else {
            return url;
        }
    }
}
