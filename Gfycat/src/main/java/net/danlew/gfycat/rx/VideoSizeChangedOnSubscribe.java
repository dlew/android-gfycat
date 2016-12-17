package net.danlew.gfycat.rx;

import android.media.MediaPlayer;
import rx.Observable;
import rx.Subscriber;
import rx.android.MainThreadSubscription;

import static rx.android.MainThreadSubscription.verifyMainThread;

public class VideoSizeChangedOnSubscribe implements Observable.OnSubscribe<MediaPlayer> {

    private final MediaPlayer mMediaPlayer;

    public VideoSizeChangedOnSubscribe(MediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    @Override
    public void call(Subscriber<? super MediaPlayer> subscriber) {
        verifyMainThread();

        mMediaPlayer.setOnVideoSizeChangedListener((mp, __, ___) -> {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext(mp);
            }
        });

        subscriber.add(new MainThreadSubscription() {
            @Override
            protected void onUnsubscribe() {
                mMediaPlayer.setOnVideoSizeChangedListener(null);
            }
        });
    }
}
