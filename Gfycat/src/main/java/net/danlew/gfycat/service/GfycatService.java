package net.danlew.gfycat.service;

import net.danlew.gfycat.model.ConvertGif;
import net.danlew.gfycat.model.GfyMetadata;
import net.danlew.gfycat.model.UrlCheck;
import retrofit.RestAdapter;
import retrofit.android.AndroidLog;
import rx.Observable;

import java.util.Random;

public class GfycatService {

    // Key will be 5-10 digits
    private static final long MIN_KEY = 10000L;
    private static final long MAX_KEY = 9999999999L;

    // Annoying, Gfycat has two different endpoints for their API atm
    private IGfycatService mService;
    private IGfycatConvertService mConvertService;

    private Random mRandom;

    public GfycatService() {
        mConvertService = new RestAdapter.Builder()
            .setEndpoint("http://upload.gfycat.com/")
            .setLogLevel(RestAdapter.LogLevel.FULL)
            .setLog(new AndroidLog("zzz"))
            .build()
            .create(IGfycatConvertService.class);

        mService = new RestAdapter.Builder()
            .setEndpoint("http://gfycat.com/")
            .setLogLevel(RestAdapter.LogLevel.FULL)
            .setLog(new AndroidLog("zzz"))
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
