package net.danlew.gfycat.service;

import android.content.Context;
import android.text.TextUtils;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import net.danlew.gfycat.GfycatApplication;
import net.danlew.gfycat.model.ConvertGif;
import net.danlew.gfycat.model.GfyItem;
import net.danlew.gfycat.model.GfyMetadata;
import net.danlew.gfycat.model.UrlCheck;
import retrofit.RestAdapter;
import retrofit.android.AndroidLog;
import retrofit.client.OkClient;
import rx.Observable;

import java.io.File;
import java.util.Random;

public class GfycatService {

    // Key will be 5-10 digits
    private static final long MIN_KEY = 10000L;
    private static final long MAX_KEY = 9999999999L;

    // Annoying, Gfycat has two different endpoints for their API atm
    private IGfycatService mService;
    private IGfycatConvertService mConvertService;

    private Random mRandom;

    public GfycatService(Context context) {
        OkHttpClient okHttpClient = new OkHttpClient();
        File cacheDir = context.getCacheDir();
        Cache cache = new Cache(cacheDir, 1024);
        okHttpClient.setCache(cache);

        OkClient client = new OkClient(okHttpClient);

        mConvertService = new RestAdapter.Builder()
            .setClient(client)
            .setEndpoint("http://upload.gfycat.com/")
            .setLogLevel(RestAdapter.LogLevel.BASIC)
            .setLog(new AndroidLog(GfycatApplication.TAG))
            .build()
            .create(IGfycatConvertService.class);

        mService = new RestAdapter.Builder()
            .setClient(client)
            .setEndpoint("http://gfycat.com/")
            .setLogLevel(RestAdapter.LogLevel.BASIC)
            .setLog(new AndroidLog(GfycatApplication.TAG))
            .build()
            .create(IGfycatService.class);

        mRandom = new Random();
    }

    // Public-facing API

    public Observable<GfyItem> getGfyItem(final String gifUrl, final String gfyName) {
        // Use the first source that returns a valid name
        return Observable.concat(
                // We already have the name
                Observable.just(gfyName),

                // We check for a pre-converted gif (for the gfyname)
                getPreExistingGfyName(gifUrl),

                // We need to convert the gif (then retrieve the gfyname)
                convertGifToGfyName(gifUrl)
            )
            .first(result -> !TextUtils.isEmpty(result))
            .flatMap(this::getMetadata)
            .map(GfyMetadata::getGfyItem);
    }

    // Network calls

    private Observable<String> getPreExistingGfyName(final String gifUrl) {
        return mService.checkUrl(gifUrl)
            .map(UrlCheck::getGfyName);
    }

    private Observable<String> convertGifToGfyName(final String gifUrl) {
        String randomString = Long.toString((long) Math.floor((mRandom.nextDouble() * (MAX_KEY - MIN_KEY)) + MIN_KEY));
        return mConvertService.convertGif(randomString, gifUrl)
            .map(ConvertGif::getGfyName);
    }

    private Observable<GfyMetadata> getMetadata(String gfyName) {
        return mService.getMetadata(gfyName);
    }
}
