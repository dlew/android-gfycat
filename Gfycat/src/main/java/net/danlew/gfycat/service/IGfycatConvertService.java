package net.danlew.gfycat.service;

import net.danlew.gfycat.model.ConvertGif;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

public interface IGfycatConvertService {

    @GET("/transcode/{random}")
    Observable<ConvertGif> convertGif(@Path("random") String randomString, @Query("fetchUrl") String encodedUrl);

}
