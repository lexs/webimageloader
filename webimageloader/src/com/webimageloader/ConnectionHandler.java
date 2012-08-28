package com.webimageloader;

import java.net.HttpURLConnection;

/**
 * Interface for accessing a {@link HttpURLConnection} before a request is
 * made, this can be used for example doing authentication. This handler is
 * only used for http(s) connections.
 *
 * @author Alexander Blom <alexanderblom.se>
 */
public interface ConnectionHandler {
    /**
     * Called before a request. Note that this method is called from a
     * background thread.
     * @param connection the connection
     */
    void handleConnection(HttpURLConnection connection);
}
