package com.webimageloader.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private MessageDigest digester;

    public Hasher() {
        try {
            digester = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            digester = null;
        }
    }

    /**
     * A hashing method that changes a string (like a URL) into a hash suitable
     * for using as a disk filename.
     */
    public String hash(String key) {
        if (digester != null) {
            byte[] bytes = digester.digest(key.getBytes());
            return bytesToHexString(bytes);
        } else {
            return String.valueOf(key.hashCode());
        }
    }

    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/a/5446120/253583
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_CHARS[(b & 0xF0) >> 4]);
            sb.append(HEX_CHARS[b & 0x0F]);
        }

        return sb.toString();
    }
}
