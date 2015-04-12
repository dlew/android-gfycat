package net.danlew.gfycat.rx;

import android.graphics.SurfaceTexture;
import android.view.TextureView;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.AndroidSubscriptions;

public class TextureChangeOnSubscribe implements Observable.OnSubscribe<SurfaceTextureEvent> {

    private final TextureView mTextureView;

    public TextureChangeOnSubscribe(TextureView textureView) {
        mTextureView = textureView;
    }

    @Override
    public void call(Subscriber<? super SurfaceTextureEvent> subscriber) {
        TextureView.SurfaceTextureListener listener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                subscriber.onNext(new SurfaceTextureEvent(SurfaceTextureEvent.Type.AVAILABLE, surface, width, height));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                subscriber.onNext(
                    new SurfaceTextureEvent(SurfaceTextureEvent.Type.SIZE_CHANGED, surface, width, height));
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                subscriber.onNext(new SurfaceTextureEvent(SurfaceTextureEvent.Type.UPDATED, surface));
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                subscriber.onNext(new SurfaceTextureEvent(SurfaceTextureEvent.Type.DESTROYED, surface));
                return true;
            }
        };

        Subscription subscription =
            AndroidSubscriptions.unsubscribeInUiThread(() -> mTextureView.setSurfaceTextureListener(null));

        subscriber.add(subscription);

        mTextureView.setSurfaceTextureListener(listener);
    }
}
