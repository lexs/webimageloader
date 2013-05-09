package com.webimageloader.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * A factory of {@link InputStream}s
 *
 * @author Alexander Blom <alexanderblom.se>
 */
public interface InputSupplier {
    /**
     * Get the length of the supplied {@link InputStream}
     *
     * @return the length
     * @throws IOException if opening the stream or file failed
     */
    long getLength() throws IOException;

    /**
     * Open a new {@link InputStream}, you should be sure to close any previously
     * opened streams before
     *
     * @return a fresh stream which you are responsible for closing
     * @throws IOException if opening the stream failed
     */
    InputStream getInput() throws IOException;
}
