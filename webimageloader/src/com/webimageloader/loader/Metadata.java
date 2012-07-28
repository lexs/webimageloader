package com.webimageloader.loader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Metadata {
    private String contentType;
    private long lastModified;
    private long expires;

    public static Metadata from(InputStream is) throws IOException {
        DataInputStream stream = new DataInputStream(is);

        String contentType = stream.readUTF();
        long lastModified = stream.readLong();
        long expires = stream.readLong();

        return new Metadata(contentType, lastModified, expires);
    }

    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream stream = new DataOutputStream(os);

        stream.writeUTF(contentType);
        stream.writeLong(lastModified);
        stream.writeLong(expires);
    }

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
