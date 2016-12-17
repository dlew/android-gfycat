package net.danlew.gfycat.rx;

import android.media.MediaPlayer;
import rx.Observable;
import rx.Subscriber;
import rx.android.MainThreadSubscription;

import static rx.android.MainThreadSubscription.verifyMainThread;

public class MediaPlayerErrorOnSubscribe implements Observable.OnSubscribe<MediaPlayerErrorEvent> {

    private final MediaPlayer mMediaPlayer;

    public MediaPlayerErrorOnSubscribe(MediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    @Override
    public void call(Subscriber<? super MediaPlayerErrorEvent> subscriber) {
        verifyMainThread();

        mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext(new MediaPlayerErrorEvent(mp, what, extra));
                return true;
            }
            return false;
        });

        subscriber.add(new MainThreadSubscription() {
            @Override
            protected void onUnsubscribe() {
                mMediaPlayer.setOnErrorListener(null);
            }
        });
    }
}
