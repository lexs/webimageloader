package com.webimageloader;

import com.webimageloader.transformation.Transformation;

public class Request {
    private String url;
    private Transformation transformation;

    private String cacheKey = null;

    public Request(String url) {
        this(url, null);
    }

    public Request(String url, Transformation transformation) {
        if (url == null) {
            throw new NullPointerException("url may not be null");
        }

        this.url = url;
        this.transformation = transformation;
    }

    public Request withoutTransformation() {
        return new Request(url);
    }

    public String getUrl() {
        return url;
    }

    public Transformation getTransformation() {
        return transformation;
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
        final int prime = 31;
        int result = 1;
        result = prime * result + getCacheKey().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Request)) {
            return false;
        }
        Request other = (Request) obj;
        if (!getCacheKey().equals(other.getCacheKey())) {
            return false;
        }
        return true;
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
