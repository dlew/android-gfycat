package net.danlew.gfycat.model;

import com.google.gson.annotations.SerializedName;

public class ConvertGif implements Gfy {

    @SerializedName("gfyname")
    private String mGfyName;

    public String getGfyName() {
        return mGfyName;
    }
}
