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
    private String etag;

    public static Metadata from(InputStream is) throws IOException {
        DataInputStream stream = new DataInputStream(is);

        String contentType = stream.readUTF();
        long lastModified = stream.readLong();
        long expires = stream.readLong();
        String etag = stream.readUTF();

        return new Metadata(contentType, lastModified, expires, etag);
    }

    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream stream = new DataOutputStream(os);

        stream.writeUTF(contentType);
        stream.writeLong(lastModified);
        stream.writeLong(expires);
        stream.writeUTF(etag);
    }

    public Metadata(String contentType, long lastModified, long expires, String etag) {
        // Don't allow strings to be null, this causes errors
        // when we write it to the outputstream later on
        if (contentType == null) {
            contentType = "";
        }

        if (etag == null) {
            etag = "";
        }

        this.contentType = contentType;
        this.lastModified = lastModified;
        this.expires = expires;
        this.etag = etag;
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

    public String getEtag() {
        return etag;
    }
}
