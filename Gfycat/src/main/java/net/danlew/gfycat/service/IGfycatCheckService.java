package net.danlew.gfycat.service;

import net.danlew.gfycat.model.UrlCheck;
import retrofit.http.GET;
import retrofit.http.Path;
import rx.Observable;

public interface IGfycatCheckService {

    @GET("/cajax/checkUrl/{encoded_url}")
    Observable<UrlCheck> checkUrl(@Path("encoded_url") String encodedUrl);

}
