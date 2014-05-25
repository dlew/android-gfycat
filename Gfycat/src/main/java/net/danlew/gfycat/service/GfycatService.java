package net.danlew.gfycat.service;

import net.danlew.gfycat.model.ConvertGif;
import retrofit.RestAdapter;
import rx.Observable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Random;

public class GfycatService {

    // Key will be 5-10 digits
    private static final long MIN_KEY = 10000L;
    private static final long MAX_KEY = 9999999999L;

    private IGfycatService mService;

    private Random mRandom;

    public GfycatService() {
        RestAdapter restAdapter = new RestAdapter.Builder().build();
        mService = restAdapter.create(IGfycatService.class);
        mRandom = new Random();
    }

    public Observable<ConvertGif> convertGif(String url) {
        String randomString = Long.toString((long) Math.floor((mRandom.nextDouble() * (MAX_KEY - MIN_KEY)) + MIN_KEY));
        String encodedUrl = encodeUrl(url);
        return mService.convertGif(randomString, encodedUrl);
    }

    public Observable<ConvertGif> checkUrl(String url) {
        String encodedUrl = encodeUrl(url);
        return mService.checkUrl(encodedUrl);
    }

    private static String encodeUrl(String url) {
        try {
            return URLEncoder.encode(url, "utf-8");
        }
        catch (UnsupportedEncodingException e) {
            // This should never happen
            throw new RuntimeException(e);
        }
    }


}
