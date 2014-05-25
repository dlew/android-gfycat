package net.danlew.gfycat.service;

import net.danlew.gfycat.model.GfyMetadata;
import net.danlew.gfycat.model.UrlCheck;
import retrofit.http.GET;
import retrofit.http.Path;
import rx.Observable;

public interface IGfycatService {

    @GET("/cajax/checkUrl/{url}")
    Observable<UrlCheck> checkUrl(@Path("url") String url);

    @GET("/cajax/get/{name}")
    Observable<GfyMetadata> getMetadata(@Path("name") String gfyName);

}
