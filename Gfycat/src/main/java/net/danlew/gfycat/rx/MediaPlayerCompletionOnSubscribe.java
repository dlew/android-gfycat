package net.danlew.gfycat.rx;

import android.media.MediaPlayer;
import rx.Observable;
import rx.Subscriber;
import rx.android.MainThreadSubscription;

import static rx.android.MainThreadSubscription.verifyMainThread;

public class MediaPlayerCompletionOnSubscribe implements Observable.OnSubscribe<MediaPlayer> {

    private final MediaPlayer mMediaPlayer;

    public MediaPlayerCompletionOnSubscribe(MediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    @Override
    public void call(Subscriber<? super MediaPlayer> subscriber) {
        verifyMainThread();

        mMediaPlayer.setOnCompletionListener(mp -> {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext(mp);
            }
        });

        subscriber.add(new MainThreadSubscription() {
            @Override
            protected void onUnsubscribe() {
                mMediaPlayer.setOnCompletionListener(null);
            }
        });

    }
}
