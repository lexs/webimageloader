package se.alexanderblom.imageloader.util;

import android.os.Build;

public class Android {
    /**
     * Check the api level of the device we're running on
     * @param level API level
     * @return  true if same or higher
     */
    public static boolean isAPI(int level) {
        return Build.VERSION.SDK_INT >= level;
    }

    private Android() {}
}
