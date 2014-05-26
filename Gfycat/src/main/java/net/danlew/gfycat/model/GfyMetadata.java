package net.danlew.gfycat.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

public class GfyMetadata implements Parcelable {

    @SerializedName("gfyItem")
    private GfyItem mGfyItem;

    public GfyMetadata() {
        // Default constructor
    }

    public GfyItem getGfyItem() {
        return mGfyItem;
    }

    //////////////////////////////////////////////////////////////////////////
    // Parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mGfyItem, 0);
    }

    private GfyMetadata(Parcel in) {
        this.mGfyItem = in.readParcelable(((Object) mGfyItem).getClass().getClassLoader());
    }

    public static final Parcelable.Creator<GfyMetadata> CREATOR = new Parcelable.Creator<GfyMetadata>() {
        public GfyMetadata createFromParcel(Parcel source) {
            return new GfyMetadata(source);
        }

        public GfyMetadata[] newArray(int size) {
            return new GfyMetadata[size];
        }
    };
}
