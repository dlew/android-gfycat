package net.danlew.gfycat.rx;

import android.media.MediaPlayer;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.AndroidSubscriptions;

public class MediaPlayerPreparedOnSubscribe implements Observable.OnSubscribe<MediaPlayer> {

    private final MediaPlayer mMediaPlayer;

    public MediaPlayerPreparedOnSubscribe(MediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    @Override
    public void call(Subscriber<? super MediaPlayer> subscriber) {
        Subscription subscription =
            AndroidSubscriptions.unsubscribeInUiThread(() -> mMediaPlayer.setOnPreparedListener(null));
        subscriber.add(subscription);

        mMediaPlayer.setOnPreparedListener(mp -> subscriber.onNext(mp));
    }
}
