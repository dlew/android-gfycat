package net.danlew.gfycat.rx;

import android.media.MediaPlayer;
import com.jakewharton.rxbinding.internal.MainThreadSubscription;
import rx.Observable;
import rx.Subscriber;

import static com.jakewharton.rxbinding.internal.Preconditions.checkUiThread;

public class MediaPlayerCompletionOnSubscribe implements Observable.OnSubscribe<MediaPlayer> {

    private final MediaPlayer mMediaPlayer;

    public MediaPlayerCompletionOnSubscribe(MediaPlayer mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    @Override
    public void call(Subscriber<? super MediaPlayer> subscriber) {
        checkUiThread();

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
