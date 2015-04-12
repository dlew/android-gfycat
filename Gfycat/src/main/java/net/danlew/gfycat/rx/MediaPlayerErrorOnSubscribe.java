package net.danlew.gfycat.rx;

import android.media.MediaPlayer;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.AndroidSubscriptions;

public class MediaPlayerErrorOnSubscribe implements Observable.OnSubscribe<MediaPlayerErrorEvent> {

    private final MediaPlayer mMediaPlayer;

    public MediaPlayerErrorOnSubscribe(MediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    @Override
    public void call(Subscriber<? super MediaPlayerErrorEvent> subscriber) {
        Subscription subscription =
            AndroidSubscriptions.unsubscribeInUiThread(() -> mMediaPlayer.setOnErrorListener(null));
        subscriber.add(subscription);

        mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
            subscriber.onNext(new MediaPlayerErrorEvent(mp, what, extra));
            return true;
        });
    }
}
