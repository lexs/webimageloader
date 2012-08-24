package com.webimageloader.util;

import java.io.IOException;
import java.io.InputStream;

public interface InputSupplier {
    InputStream getInput() throws IOException;
}
