package net.danlew.gfycat.rx;

import android.media.MediaPlayer;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.AndroidSubscriptions;

public class MediaPlayerCompletionOnSubscribe implements Observable.OnSubscribe<MediaPlayer> {

    private final MediaPlayer mMediaPlayer;

    public MediaPlayerCompletionOnSubscribe(MediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    @Override
    public void call(Subscriber<? super MediaPlayer> subscriber) {
        Subscription subscription =
            AndroidSubscriptions.unsubscribeInUiThread(() -> mMediaPlayer.setOnCompletionListener(null));
        subscriber.add(subscription);

        mMediaPlayer.setOnCompletionListener(mp -> subscriber.onNext(mp));
    }
}
