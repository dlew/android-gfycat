package net.danlew.gfycat.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ProgressBar;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.crashlytics.android.Crashlytics;
import net.danlew.gfycat.GfycatApplication;
import net.danlew.gfycat.Log;
import net.danlew.gfycat.R;
import net.danlew.gfycat.Stats;
import net.danlew.gfycat.model.GfyItem;
import net.danlew.gfycat.rx.MediaPlayerCompletionOnSubscribe;
import net.danlew.gfycat.rx.MediaPlayerErrorOnSubscribe;
import net.danlew.gfycat.rx.MediaPlayerPreparedOnSubscribe;
import net.danlew.gfycat.rx.SurfaceTextureEvent;
import net.danlew.gfycat.rx.TextureChangeOnSubscribe;
import net.danlew.gfycat.rx.VideoSizeChangedOnSubscribe;
import net.danlew.gfycat.service.GfycatService;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This Activity takes a GIF URL and converts it to Gfycat.
 *
 * It's optimized to avoid having to restart the stream, so it handles its
 * own common configuration changes (e.g. orientation).
 */
public class MainActivity extends Activity implements ErrorDialog.IListener {

    private static final String INSTANCE_CURRENT_POSITION = "INSTANCE_CURRENT_POSITION";
    private static final String INSTANCE_RECORDED_STATS = "INSTANCE_RECORDED_STATS";

    @Inject
    GfycatService mGfycatService;

    @InjectView(R.id.container)
    ViewGroup mContainer;

    @InjectView(R.id.progress_bar)
    ProgressBar mProgressBar;

    @InjectView(R.id.video_progress_bar)
    ProgressBar mVideoProgressBar;

    @InjectView(R.id.video_view)
    TextureView mVideoView;

    private String mGifUrl;
    private String mGfyName;

    private MediaPlayer mMediaPlayer;
    private boolean mMediaPlayerPrepared;
    private int mCurrentPosition;

    private Subscription mLoadVideoSubscription;
    private Subscription mVideoProgressBarSubscription;

    private Observable<SurfaceTextureEvent> mSurfaceTextureEvents;

    private GestureDetector mGestureDetector;

    // Used to detect if we clicked inside the running video
    private RectF mVideoRect;

    private boolean mRecordedStats;

    //////////////////////////////////////////////////////////////////////////
    // Lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCurrentPosition = savedInstanceState.getInt(INSTANCE_CURRENT_POSITION);
            mRecordedStats = savedInstanceState.getBoolean(INSTANCE_RECORDED_STATS);
        }

        GfycatApplication.get(this).inject(this);

        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        mGestureDetector = new GestureDetector(this, mOnGestureListener);

        mSurfaceTextureEvents = Observable.create(new TextureChangeOnSubscribe(mVideoView)).share();

        // Correct aspect ratio if the video size changes
        mSurfaceTextureEvents
            .filter(event -> event.getType() == SurfaceTextureEvent.Type.SIZE_CHANGED)
            .subscribe(__ -> correctVideoAspectRatio());

        // Load the gfyname/gifurl from the intent provided
        if (mGfyName == null && mGifUrl == null) {
            Intent intent = getIntent();

            // Handle SEND Intent
            if (Intent.ACTION_SEND.equals(intent.getAction())) {
                CharSequence sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                if (!TextUtils.isEmpty(sharedText) && URLUtil.isNetworkUrl(sharedText.toString())) {
                    mGifUrl = sharedText.toString();
                }
            }

            // If this is an actual gfycat link, extract the name
            else {
                // If this is an actual gfycat link, extract the name
                Uri data = intent.getData();
                if (data.getHost() != null && data.getHost().endsWith("gfycat.com")) {
                    List<String> pathSegments = data.getPathSegments();
                    if (pathSegments.size() == 0) {
                        // They've gone to gfycat.com itself; not sure yet how to disclude that URL,
                        // so just show an error dialog for now.
                        showErrorDialog();
                    }
                    else if (pathSegments.size() == 1) {
                        mGfyName = pathSegments.get(0);
                    }
                    else if (pathSegments.size() > 1 && pathSegments.get(0).equals("fetch")) {
                        String strUrl = data.toString();
                        mGifUrl = strUrl.substring(strUrl.indexOf("fetch") + 6);
                    }
                }
                else {
                    mGifUrl = data.toString();
                }
            }

            if (mGifUrl == null && mGfyName == null) {
                showErrorDialog();
                return;
            }
        }

        // Fade in the background; looks nicer
        if (savedInstanceState == null) {
            mContainer.setAlpha(0);
            mContainer.animate().alpha(1);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Begin a load, unless there was an error
        if (getFragmentManager().findFragmentByTag(ErrorDialog.TAG) == null) {
            loadGfy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mMediaPlayer != null && mMediaPlayerPrepared) {
            outState.putInt(INSTANCE_CURRENT_POSITION, mMediaPlayer.getCurrentPosition());
        }

        outState.putBoolean(INSTANCE_RECORDED_STATS, mRecordedStats);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Reset the state of the entire Activity, prepare to reload everything in onStart()
        if (mLoadVideoSubscription != null) {
            mLoadVideoSubscription.unsubscribe();
        }
        if (mVideoProgressBarSubscription != null) {
            mVideoProgressBarSubscription.unsubscribe();
        }

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
            mVideoRect = null;
        }

        mProgressBar.setVisibility(View.VISIBLE);
        mVideoProgressBar.setVisibility(View.INVISIBLE);
        // TODO: Figure out how to reset TextureViews
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // For some reason, sometimes the back button doesn't finish this activity (seen it in the wild).
        finish();
    }

    //////////////////////////////////////////////////////////////////////////
    // RxJava

    private void loadGfy() {
        mLoadVideoSubscription = mSurfaceTextureEvents
            .filter(event -> event.getType() == SurfaceTextureEvent.Type.AVAILABLE)
            .take(1)
            .flatMap(__ -> mGfycatService.getGfyItem(mGifUrl, mGfyName))
            .map(this::createMediaPlayerForGfyItem)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                mediaPlayer -> {
                    mMediaPlayer = mediaPlayer;
                    mMediaPlayerPrepared = false;
                    mediaPlayer.prepareAsync();
                },
                throwable -> {
                    Log.e("Could not display GIF", throwable);
                    Crashlytics.logException(throwable);
                    showErrorDialog();
                }
            );
    }

    //////////////////////////////////////////////////////////////////////////
    // MediaPlayer

    private MediaPlayer createMediaPlayerForGfyItem(final GfyItem gfyItem) {
        MediaPlayer mediaPlayer = new MediaPlayer();

        mediaPlayer.setLooping(true);

        Observable.create(new VideoSizeChangedOnSubscribe(mediaPlayer))
            .subscribe(__ -> correctVideoAspectRatio());

        Observable.create(new MediaPlayerPreparedOnSubscribe(mediaPlayer))
            .subscribe(mp -> {
                mProgressBar.setVisibility(View.GONE);
                mp.start();
                mp.seekTo(mCurrentPosition);

                // Only set the progress bar visible if the duration is > 1000ms
                if (mp.getDuration() > 1000) {
                    mVideoProgressBar.setVisibility(View.VISIBLE);
                    mVideoProgressBar.setProgress(mCurrentPosition);

                    mVideoProgressBar.setMax(mp.getDuration());

                    mVideoProgressBarSubscription = Observable.interval(10, TimeUnit.MILLISECONDS)
                        .filter(__ -> mMediaPlayerPrepared)
                        .map(__ -> mp.getCurrentPosition())
                        .subscribe(progress -> mVideoProgressBar.setProgress(progress),
                            throwable -> {
                                Crashlytics.logException(throwable);
                                showErrorDialog();
                            });
                }
                else {
                    mVideoProgressBar.setVisibility(View.INVISIBLE);
                }

                if (!mRecordedStats) {
                    Stats stats = new Stats(MainActivity.this);
                    stats.addItem(gfyItem);
                    mRecordedStats = true;
                }

                mMediaPlayerPrepared = true;
            });

        // Enable looping for Samsung devices, tested on Galaxy S5 (4.4.4)
        Observable.create(new MediaPlayerCompletionOnSubscribe(mediaPlayer))
            .subscribe(completedMediaPlayer -> {
                completedMediaPlayer.pause();
                completedMediaPlayer.seekTo(0);
                completedMediaPlayer.start();
            });

        // Stop what we're doing in case of an error
        Observable.create(new MediaPlayerErrorOnSubscribe(mediaPlayer))
            .subscribe(errorEvent -> {
                mMediaPlayerPrepared = false;
                Crashlytics.log("MediaPlayer error; what=" + errorEvent.getWhat() + " extra=" + errorEvent.getExtra());
                showErrorDialog();
            });

        try {
            mediaPlayer.setDataSource(gfyItem.getWebmUrl());
            mediaPlayer.setSurface(new Surface(mVideoView.getSurfaceTexture()));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        return mediaPlayer;
    }

    // We want to make sure the aspect ratio is correct; we can do that easily by scaling the TextureView
    // to the correct size.
    private void correctVideoAspectRatio() {
        if (mMediaPlayer == null) {
            return;
        }

        int dwidth = mMediaPlayer.getVideoWidth();
        int dheight = mMediaPlayer.getVideoHeight();

        float scaleX;
        float scaleY;

        // We want to figure out which dimension will fill; then scale the other one so it maintains aspect ratio
        int vwidth = mVideoView.getWidth();
        int vheight = mVideoView.getHeight();

        float ratioX = (float) vwidth / (float) dwidth;
        float ratioY = (float) vheight / (float) dheight;
        if (ratioX < ratioY) {
            scaleX = 1;
            float desiredHeight = ratioX * dheight;
            scaleY = desiredHeight / (float) vheight;
        }
        else {
            float desiredWidth = ratioY * dwidth;
            scaleX = desiredWidth / (float) vwidth;
            scaleY = 1;
        }

        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, vwidth / 2f, vheight / 2f);
        mVideoView.setTransform(matrix);

        mVideoRect = new RectF(0, 0, vwidth, vheight);

        matrix.mapRect(mVideoRect);

        // Update the location of the video progress bar
        int actualHeight = (int) (vheight * scaleY);
        int videoBottom = vheight - (vheight - actualHeight) / 2;
        int offset = getResources().getDimensionPixelSize(R.dimen.video_progress_bar_offset);

        mVideoProgressBar.setScaleX(scaleX);
        mVideoProgressBar.setTranslationY(videoBottom + offset);
    }

    //////////////////////////////////////////////////////////////////////////
    // Error dialog

    private void showErrorDialog() {
        // If possible, smoothly get rid of our screen before showing error dialog
        mProgressBar.animate().alpha(0);
        mContainer.animate().alpha(0);

        getAlternatives().subscribe(
            intents -> {
                if (intents.isEmpty()) {
                    ErrorDialog.newInstance().show(getFragmentManager(), "error");
                }
                else {
                    List<LabeledIntent> mutableIntentList = new ArrayList<>(intents);
                    Intent chooserIntent =
                        Intent.createChooser(mutableIntentList.remove(0), getString(R.string.error_alternatives));
                    chooserIntent
                        .putExtra(Intent.EXTRA_INITIAL_INTENTS, mutableIntentList.toArray(new Parcelable[] { }));
                    startActivity(chooserIntent);
                    finish();
                }
            }
        );
    }

    private Observable<List<LabeledIntent>> getAlternatives() {
        final PackageManager packageManager = getPackageManager();
        final String packageName = getPackageName();

        final Intent intent = getIntent();
        Intent dummy = new Intent(intent);
        dummy.setComponent(null);

        return Observable.from(packageManager.queryIntentActivities(dummy, 0))
            .filter(resolveInfo -> !TextUtils.equals(resolveInfo.activityInfo.packageName, packageName))
            .map(resolveInfo -> {
                Intent alternative = new Intent(intent);
                alternative.setPackage(resolveInfo.activityInfo.packageName);
                alternative.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);

                LabeledIntent labeledAlternative = new LabeledIntent(alternative,
                    resolveInfo.activityInfo.packageName, resolveInfo.loadLabel(packageManager), resolveInfo.icon);

                return labeledAlternative;
            })
            .toSortedList((labeledIntent, labeledIntent2) -> {
                String label = labeledIntent.getNonLocalizedLabel().toString();
                String label2 = labeledIntent2.getNonLocalizedLabel().toString();
                return label.compareTo(label2);
            });
    }

    //////////////////////////////////////////////////////////////////////////
    // Gesture detection

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);

        return super.onTouchEvent(event);
    }

    private GestureDetector.OnGestureListener mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            // We want to close the screen if the user clicks on anything but the
            // animating GIF itself.
            if (mVideoRect == null || !mVideoRect.contains(e.getX(), e.getY())) {
                finish();
                return true;
            }

            return false;
        }
    };

    //////////////////////////////////////////////////////////////////////////
    // ErrorDialog.IListener

    @Override
    public void onDismiss() {
        finish();
    }
}
