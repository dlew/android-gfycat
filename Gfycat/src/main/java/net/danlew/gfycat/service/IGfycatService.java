package net.danlew.gfycat.service;

import net.danlew.gfycat.model.ConvertGif;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

public interface IGfycatService {

    @GET("http://upload.gfycat.com/transcode/{random}")
    Observable<ConvertGif> convertGif(@Path("random") String randomString, @Query("fetchUrl") String encodedUrl);

    @GET("http://gfycat.com/cajax/checkUrl/{encoded_url}")
    Observable<ConvertGif> checkUrl(@Path("encoded_url") String encodedUrl);

}
