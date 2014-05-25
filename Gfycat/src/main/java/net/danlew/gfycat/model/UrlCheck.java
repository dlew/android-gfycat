package net.danlew.gfycat.model;

import com.google.gson.annotations.SerializedName;

public class UrlCheck implements Gfy {

    @SerializedName("urlKnown")
    private boolean mUrlKnown;

    @SerializedName("gfyName")
    private String mGfyName;

    public boolean isUrlKnown() {
        return mUrlKnown;
    }

    public String getGfyName() {
        return mGfyName;
    }

    @Override
    public String toString() {
        return "UrlCheck{" +
            "mUrlKnown=" + mUrlKnown +
            ", mGfyName='" + mGfyName + '\'' +
            '}';
    }
}
