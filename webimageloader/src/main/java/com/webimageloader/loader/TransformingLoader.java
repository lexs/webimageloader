package com.webimageloader.loader;

import android.graphics.Bitmap;
import android.util.Log;

import com.webimageloader.ImageLoader.Logger;
import com.webimageloader.transformation.Transformation;
import com.webimageloader.util.BitmapUtils;
import com.webimageloader.util.InputSupplier;

import java.io.IOException;

public class TransformingLoader implements Loader {
    private static final String TAG = "TransformingLoader";

    @Override
    public void load(final LoaderWork.Manager manager, LoaderRequest request) {
        if (Logger.VERBOSE) Log.v(TAG, "Transforming " + request);

        final Transformation transformation = request.getTransformation();

        // Modify request
        LoaderRequest modified = request.withoutTransformation();
        manager.next(modified, new Listener() {
            @Override
            public void onStreamLoaded(InputSupplier input, Metadata metadata) {
                try {
                    Bitmap transformedBitmap = transformation.transform(input);
                    Metadata transformedMetadata = getTransformedMetadata(metadata, transformation);

                    deliverResult(transformedBitmap, transformedMetadata);
                } catch (IOException e) {
                    manager.deliverError(e);
                }
            }

            @Override
            public void onBitmapLoaded(Bitmap b, Metadata metadata) {
                b = transformation.transform(b);
                deliverResult(b, metadata);
            }

            private void deliverResult(Bitmap b, Metadata metadata) {
                if (b == null) {
                    onError(new IllegalStateException("Transformer returned null"));
                } else {
                    manager.deliverBitmap(b, metadata);
                }
            }

            @Override
            public void onNotModified(Metadata metadata) {
                manager.deliverNotMotified(metadata);
            }

            @Override
            public void onError(Throwable t) {
                manager.deliverError(t);
            }
            
			@Override
			public void onProgress(int progress) {
				manager.deliverProgress(progress);
			}
        });
    }

    private Metadata getTransformedMetadata(Metadata metadata, Transformation transformation) {
        Bitmap.CompressFormat format = transformation.getCompressFormat();
        if (format == null) {
            // Transformed loader doesn't care about format, use the same
            return metadata;
        }

        String contentType = BitmapUtils.getContentType(format);
        return new Metadata(contentType, metadata.getLastModified(), metadata.getExpires(), metadata.getEtag());
    }
}