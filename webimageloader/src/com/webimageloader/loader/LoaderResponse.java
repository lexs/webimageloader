package com.webimageloader.loader;

public class LoaderResponse<T> {
    private T value;

    private String contentType;
    private long lastModified;
    private long expires;

    public static <T> LoaderResponse<T> create(T value, String contentType, long lastModified, long expires) {
        return new LoaderResponse<T>(value, contentType, lastModified, expires);
    }

    public static <T1, T2> LoaderResponse<T1> create(T1 value, LoaderResponse<T2> parent) {
        return new LoaderResponse<T1>(value, parent.contentType, parent.lastModified, parent.expires);
    }

    private LoaderResponse(T value, String contentType, long lastModified, long expires) {
        this.contentType = contentType;
        this.lastModified = lastModified;
        this.value = value;
        this.expires = expires;
    }

    public T get() {
        return value;
    }
}
