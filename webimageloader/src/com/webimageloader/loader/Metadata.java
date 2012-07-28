package com.webimageloader.loader;

public class Metadata {
    private String contentType;
    private long lastModified;
    private long expires;

    public Metadata(String contentType, long lastModified, long expires) {
        this.contentType = contentType;
        this.lastModified = lastModified;
        this.expires = expires;
    }

    public String getContentType() {
        return contentType;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getExpires() {
        return expires;
    }
}
