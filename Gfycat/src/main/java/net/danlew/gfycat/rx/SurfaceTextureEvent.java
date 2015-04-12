package net.danlew.gfycat.rx;

import android.graphics.SurfaceTexture;

/**
 * Represents an event from TextureView.SurfaceTextureListener
 */
public class SurfaceTextureEvent {

    public enum Type {
        AVAILABLE,
        SIZE_CHANGED,
        UPDATED,
        DESTROYED
    }

    private final Type mType;
    private final SurfaceTexture mSurfaceTexture;
    private final int mWidth;
    private final int mHeight;

    public SurfaceTextureEvent(Type type, SurfaceTexture surfaceTexture) {
        this(type, surfaceTexture, 0, 0);
    }

    public SurfaceTextureEvent(Type type, SurfaceTexture surfaceTexture, int width, int height) {
        mType = type;
        mSurfaceTexture = surfaceTexture;
        mWidth = width;
        mHeight = height;
    }

    public Type getType() {
        return mType;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }
}
