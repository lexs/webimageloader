package com.webimageloader.sample.simple;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * An ImageView that does not request a layout pass when an image is set. This should
 * only be used when the view is of fixed size and images can not change the layout.
 *
 * See this post for more info: https://plus.google.com/113058165720861374515/posts/iTk4PjgeAWX
 */
public class FastImageView extends ImageView {
    private boolean blockLayout;

    public FastImageView(Context context) {
        super(context);
    }

    public FastImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FastImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        blockLayout = true;
        super.setImageBitmap(bm);
        blockLayout = false;
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        blockLayout = true;
        super.setImageDrawable(drawable);
        blockLayout = false;
    }

    @Override
    public void requestLayout() {
        if (!blockLayout) {
            super.requestLayout();
        }
    }
}
