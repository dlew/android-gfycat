package net.danlew.gfycat.model;

import com.google.gson.annotations.SerializedName;

public class UrlCheck implements Gfy {

    @SerializedName("gfyName")
    private String mGfyName;

    public String getGfyName() {
        return mGfyName;
    }
}
