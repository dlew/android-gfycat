package net.danlew.gfycat;

import android.content.Context;
import android.content.SharedPreferences;
import net.danlew.gfycat.model.GfyItem;

/**
 * Stores statistics about usage of Gfycat
 */
public final class Stats {

    private static final String PREFS_NAME = "Stats.Storage";

    // The total size of all gifs requested (that were converted by Gfycat)
    private static final String KEY_TOTAL_GIF_SIZE = "TotalGifSize";

    // The total size of all videos streamed
    private static final String KEY_TOTAL_WEBM_SIZE = "TotalWebmSize";

    private SharedPreferences mSharedPreferences;

    public Stats(Context context) {
        mSharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private long getTotalGifSize() {
        return mSharedPreferences.getLong(KEY_TOTAL_GIF_SIZE, 0);
    }

    private long getTotalWebmSize() {
        return mSharedPreferences.getLong(KEY_TOTAL_WEBM_SIZE, 0);
    }

    public long getTotalSavings() {
        return getTotalGifSize() - getTotalWebmSize();
    }

    public void addItem(GfyItem item) {
        long oldGifSize = getTotalGifSize();
        long oldWebmSize = getTotalWebmSize();

        mSharedPreferences.edit()
            .putLong(KEY_TOTAL_GIF_SIZE, oldGifSize + item.getGifSize())
            .putLong(KEY_TOTAL_WEBM_SIZE, oldWebmSize + item.getWebmSize())
            .apply();
    }
}
