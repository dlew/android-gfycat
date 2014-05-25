package net.danlew.gfycat.model;

import com.google.gson.annotations.SerializedName;

public class GfyMetadata {

    @SerializedName("gfyItem")
    private GfyItem mGfyItem;

    public GfyItem getGfyItem() {
        return mGfyItem;
    }
}
