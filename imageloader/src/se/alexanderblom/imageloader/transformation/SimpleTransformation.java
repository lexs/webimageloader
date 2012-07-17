package se.alexanderblom.imageloader.transformation;

import java.io.InputStream;

import se.alexanderblom.imageloader.util.BitmapUtils;
import android.graphics.Bitmap;

public abstract class SimpleTransformation implements Transformation {
    @Override
    public Bitmap transform(InputStream is) {
        Bitmap b = BitmapUtils.decodeStream(is);
        return transform(b);
    }
}
