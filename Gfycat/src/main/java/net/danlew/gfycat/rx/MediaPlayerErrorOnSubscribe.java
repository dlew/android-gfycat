package net.danlew.gfycat.rx;

import android.media.MediaPlayer;
import com.jakewharton.rxbinding.internal.MainThreadSubscription;
import rx.Observable;
import rx.Subscriber;

import static com.jakewharton.rxbinding.internal.Preconditions.checkUiThread;

public class MediaPlayerErrorOnSubscribe implements Observable.OnSubscribe<MediaPlayerErrorEvent> {

    private final MediaPlayer mMediaPlayer;

    public MediaPlayerErrorOnSubscribe(MediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    @Override
    public void call(Subscriber<? super MediaPlayerErrorEvent> subscriber) {
        checkUiThread();

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
