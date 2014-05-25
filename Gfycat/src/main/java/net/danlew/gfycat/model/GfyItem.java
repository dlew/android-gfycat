package net.danlew.gfycat.model;

import com.google.gson.annotations.SerializedName;

public class GfyItem {

    @SerializedName("width")
    private int mWidth;

    @SerializedName("height")
    private int mHeight;

    @SerializedName("webmUrl")
    private String mWebmUrl;

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public String getWebmUrl() {
        return mWebmUrl;
    }
}
