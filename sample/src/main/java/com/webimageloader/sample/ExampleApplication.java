package com.webimageloader.sample;

import com.squareup.okhttp.OkHttpClient;
import com.webimageloader.ConnectionFactory;
import com.webimageloader.ImageLoader;
import com.webimageloader.ext.ImageLoaderApplication;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class ExampleApplication extends ImageLoaderApplication {
    @Override
    protected ImageLoader.Builder getBuilder() {
        return super.getBuilder().setConnectionFactory(new OkHttpFactory());
    }

    private static class OkHttpFactory implements ConnectionFactory {
        private final OkHttpClient client = new OkHttpClient();

        @Override
        public URLConnection openConnection(URL url) throws IOException {
            return client.open(url);
        }
    }
}
