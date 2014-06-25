package net.danlew.gfycat.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

public class GfyItem implements Parcelable {

    @SerializedName("width")
    private int mWidth;

    @SerializedName("height")
    private int mHeight;

    @SerializedName("webmUrl")
    private String mWebmUrl;

    @SerializedName("gifSize")
    private long mGifSize;

    @SerializedName("webmSize")
    private long mWebmSize;

    public GfyItem() {
        // Default constructor
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public String getWebmUrl() {
        return mWebmUrl;
    }

    public long getGifSize() {
        return mGifSize;
    }

    public long getWebmSize() {
        return mWebmSize;
    }

    //////////////////////////////////////////////////////////////////////////
    // Parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mWidth);
        dest.writeInt(this.mHeight);
        dest.writeString(this.mWebmUrl);
    }

    private GfyItem(Parcel in) {
        this.mWidth = in.readInt();
        this.mHeight = in.readInt();
        this.mWebmUrl = in.readString();
    }

    public static final Parcelable.Creator<GfyItem> CREATOR = new Parcelable.Creator<GfyItem>() {
        public GfyItem createFromParcel(Parcel source) {
            return new GfyItem(source);
        }

        public GfyItem[] newArray(int size) {
            return new GfyItem[size];
        }
    };
}
