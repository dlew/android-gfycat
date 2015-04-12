package net.danlew.gfycat.rx;

import android.media.MediaPlayer;

public class MediaPlayerErrorEvent {

    private final MediaPlayer mMediaPlayer;
    private final int mWhat;
    private final int mExtra;

    public MediaPlayerErrorEvent(MediaPlayer mediaPlayer, int what, int extra) {
        mMediaPlayer = mediaPlayer;
        mWhat = what;
        mExtra = extra;
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public int getWhat() {
        return mWhat;
    }

    public int getExtra() {
        return mExtra;
    }
}
