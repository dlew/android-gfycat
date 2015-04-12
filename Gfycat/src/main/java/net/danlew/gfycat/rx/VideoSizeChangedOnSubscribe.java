package net.danlew.gfycat.rx;

import android.media.MediaPlayer;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.AndroidSubscriptions;

public class VideoSizeChangedOnSubscribe implements Observable.OnSubscribe<MediaPlayer> {

    private final MediaPlayer mMediaPlayer;

    public VideoSizeChangedOnSubscribe(MediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    @Override
    public void call(Subscriber<? super MediaPlayer> subscriber) {
        Subscription subscription =
            AndroidSubscriptions.unsubscribeInUiThread(() -> mMediaPlayer.setOnVideoSizeChangedListener(null));
        subscriber.add(subscription);

        mMediaPlayer.setOnVideoSizeChangedListener((mp, __, ___) -> subscriber.onNext(mp));
    }
}
