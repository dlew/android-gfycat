package net.danlew.gfycat.service;

import net.danlew.gfycat.model.ConvertGif;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import rx.Observable;

public interface IGfycatConvertService {

    @GET("/transcode/{random}")
    Observable<ConvertGif> convertGif(@Path("random") String randomString, @Query("fetchUrl") String encodedUrl);

}
