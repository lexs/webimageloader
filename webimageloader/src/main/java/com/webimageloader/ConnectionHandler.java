package com.webimageloader;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Interface for creating an {@link HttpURLConnection} for a request.
 *
 * This can be used, for example, for doing HTTP authentication or using a custom HttpURLConnection.
 * This handler is only used for http(s) connections.
 *
 * @author Alexander Blom <alexanderblom.se>
 */
public interface ConnectionHandler {
    /**
     * Called before a request. Note that this method is called from a background thread.
     *
     * @param url URL to be loaded.
     * @return The HTTP connection object to be used for this URL.
     */
    HttpURLConnection handleConnection(URL url) throws IOException;
}
