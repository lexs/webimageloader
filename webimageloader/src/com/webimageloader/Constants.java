package com.webimageloader;

public class Constants {
    public static final int DEFAULT_CONNECTION_TIMEOUT = 10 * 1000; // 10 sec
    public static final int DEFAULT_READ_TIMEOUT = 15 * 1000; // 15 sec
    public static final long DEFAULT_MAX_AGE = 3 * 24 * 60 * 60 * 1000; // Three days
    public static final long MAX_AGE_INFINITY = 0;
    public static final long MAX_AGE_NOT_FORCED = -1;

    private Constants() {}
}
