package net.danlew.gfycat.service;

import net.danlew.gfycat.model.ConvertGif;
import net.danlew.gfycat.model.UrlCheck;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

public interface IGfycatService {

    @GET("upload.gfycat.com/transcode/{random}")
    Observable<ConvertGif> convertGif(@Path("random") String randomString, @Query("fetchUrl") String encodedUrl);

    @GET("gfycat.com/cajax/checkUrl/{encoded_url}")
    Observable<UrlCheck> checkUrl(@Path("encoded_url") String encodedUrl);

}
