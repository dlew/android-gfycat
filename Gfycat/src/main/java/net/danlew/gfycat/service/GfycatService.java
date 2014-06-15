package net.danlew.gfycat.service;

import android.content.Context;
import com.crashlytics.android.Crashlytics;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import net.danlew.gfycat.GfycatApplication;
import net.danlew.gfycat.Log;
import net.danlew.gfycat.model.ConvertGif;
import net.danlew.gfycat.model.GfyMetadata;
import net.danlew.gfycat.model.UrlCheck;
import retrofit.RestAdapter;
import retrofit.android.AndroidLog;
import retrofit.client.OkClient;
import rx.Observable;

import java.io.File;
import java.io.IOException;
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
        try {
            File cacheDir = context.getCacheDir();
            Cache cache = new Cache(cacheDir, 1024);
            okHttpClient.setCache(cache);
        }
        catch (IOException e) {
            Log.e("Could not configure response cache", e);
            Crashlytics.logException(e);
        }

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

    public Observable<ConvertGif> convertGif(String url) {
        String randomString = Long.toString((long) Math.floor((mRandom.nextDouble() * (MAX_KEY - MIN_KEY)) + MIN_KEY));
        return mConvertService.convertGif(randomString, url);
    }

    public Observable<UrlCheck> checkUrl(String url) {
        return mService.checkUrl(url);
    }

    public Observable<GfyMetadata> getMetadata(String gfyName) {
        return mService.getMetadata(gfyName);
    }
}
