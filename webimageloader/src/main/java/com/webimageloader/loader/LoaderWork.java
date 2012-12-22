package com.webimageloader.loader;

import android.graphics.Bitmap;
import com.webimageloader.util.InputSupplier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

public class LoaderWork {
    private final Loader.Listener listener;
    private final List<Future<?>> futures;

    public LoaderWork(Loader.Listener listener) {
        this.listener = listener;
        this.futures = new ArrayList<Future<?>>();
    }

    public void cancel() {
        synchronized (futures) {
            for (Future<?> future : futures) {
                future.cancel(false);
            }
        }
    }

    public void start(List<Loader> loaderChain, LoaderRequest request) {
        Iterator<Loader> it = loaderChain.iterator();
        Loader loader = it.next();

        loader.load(new Manager(it, listener), request);
    }

    public class Manager {
        private Iterator<Loader> chain;
        private Loader.Listener listener;

        private Loader nextLoader;

        Manager(Iterator<Loader> chain, Loader.Listener listener) {
            this.chain = chain;
            this.listener = listener;

            if (chain.hasNext()) {
                nextLoader = chain.next();
            }
        }

        public void addFuture(Future<?> future) {
            synchronized (futures) {
                futures.add(future);
            }
        }

        public void next(LoaderRequest request, Loader.Listener listener) {
            Manager nextManager = new Manager(chain, listener);
            nextLoader.load(nextManager, request);
        }

        public void next(LoaderRequest request) {
            Manager nextManager = new Manager(chain, listener);
            nextLoader.load(nextManager, request);
        }

        public void deliverStream(InputSupplier is, Metadata metadata) {
            listener.onStreamLoaded(is, metadata);
        }

        public void deliverBitmap(Bitmap b, Metadata metadata) {
            listener.onBitmapLoaded(b, metadata);
        }

        public void deliverError(Throwable t) {
            listener.onError(t);
        }

        public void deliverNotMotified(Metadata metadata) {
            listener.onNotModified(metadata);
        }
    }
}
