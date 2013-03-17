package com.webimageloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Interface for creating an {@link URLConnection} for a request.
 *
 * @author Alexander Blom <alexanderblom.se>
 */
public interface ConnectionFactory {
    /**
     * Called to create the connection for a request. Note that this is
     * called from a background thread.
     * @param url the url
     * @return a new connection
     */
    URLConnection openConnection(URL url) throws IOException;
}
