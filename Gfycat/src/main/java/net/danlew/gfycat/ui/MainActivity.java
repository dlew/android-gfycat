package net.danlew.gfycat.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
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
import net.danlew.gfycat.model.ConvertGif;
import net.danlew.gfycat.model.GfyMetadata;
import net.danlew.gfycat.model.UrlCheck;
import net.danlew.gfycat.service.GfycatService;
import rx.Observable;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

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

    private static final String INSTANCE_GFY_NAME = "INSTANCE_GFY_NAME";
    private static final String INSTANCE_GFY_METADATA = "INSTANCE_GFY_METADATA";
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
    private GfyMetadata mGfyMetadata;
    private int mCurrentPosition;

    private MediaPlayer mMediaPlayer;

    private Subscription mLoadVideoSubscription;
    private Subscription mVideoProgressBarSubscription;

    private BehaviorSubject<SurfaceTexture> mSurfaceTextureSubject = BehaviorSubject.create((SurfaceTexture) null);

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
            mGfyName = savedInstanceState.getString(INSTANCE_GFY_NAME);
            mGfyMetadata = savedInstanceState.getParcelable(INSTANCE_GFY_METADATA);
            mCurrentPosition = savedInstanceState.getInt(INSTANCE_CURRENT_POSITION);
            mRecordedStats = savedInstanceState.getBoolean(INSTANCE_RECORDED_STATS);
        }

        GfycatApplication.get(this).inject(this);

        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        mGestureDetector = new GestureDetector(this, mOnGestureListener);

        mVideoView.setSurfaceTextureListener(mSurfaceTextureListener);

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
        outState.putString(INSTANCE_GFY_NAME, mGfyName);
        outState.putParcelable(INSTANCE_GFY_METADATA, mGfyMetadata);

        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
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

    private void showErrorDialog() {
        // If possible, smoothly get rid of our screen before showing error dialog
        mProgressBar.animate().alpha(0);
        mContainer.animate().alpha(0);

        getAlternatives().subscribe(
            new Action1<List<LabeledIntent>>() {
                @Override
                public void call(List<LabeledIntent> intents) {
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
            .filter(new Func1<ResolveInfo, Boolean>() {
                @Override
                public Boolean call(ResolveInfo resolveInfo) {
                    return !TextUtils.equals(resolveInfo.activityInfo.packageName, packageName);
                }
            })
            .map(new Func1<ResolveInfo, LabeledIntent>() {
                @Override
                public LabeledIntent call(ResolveInfo resolveInfo) {
                    Intent alternative = new Intent(intent);
                    alternative.setPackage(resolveInfo.activityInfo.packageName);
                    alternative.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);

                    LabeledIntent labeledAlternative = new LabeledIntent(alternative,
                        resolveInfo.activityInfo.packageName, resolveInfo.loadLabel(packageManager), resolveInfo.icon);

                    return labeledAlternative;
                }
            })
            .toSortedList(new Func2<LabeledIntent, LabeledIntent, Integer>() {
                @Override
                public Integer call(LabeledIntent labeledIntent, LabeledIntent labeledIntent2) {
                    String label = labeledIntent.getNonLocalizedLabel().toString();
                    String label2 = labeledIntent2.getNonLocalizedLabel().toString();
                    return label.compareTo(label2);
                }
            });
    }

    //////////////////////////////////////////////////////////////////////////
    // RxJava

    private Observable<String> getGfyNameObservable() {
        if (!TextUtils.isEmpty(mGfyName)) {
            return Observable.just(mGfyName);
        }

        return mGfycatService.checkUrl(mGifUrl)
            .flatMap(
                new Func1<UrlCheck, Observable<String>>() {
                    @Override
                    public Observable<String> call(UrlCheck urlCheck) {
                        if (urlCheck.isUrlKnown()) {
                            return Observable.just(urlCheck.getGfyName());
                        }

                        return mGfycatService.convertGif(mGifUrl).map(new Func1<ConvertGif, String>() {
                            @Override
                            public String call(ConvertGif convertGif) {
                                return convertGif.getGfyName();
                            }
                        });
                    }
                }
            )
            .flatMap(
                new Func1<String, Observable<? extends String>>() {
                    @Override
                    public Observable<? extends String> call(String gfyName) {
                        // Error out if the name is empty
                        if (TextUtils.isEmpty(gfyName)) {
                            return Observable.error(new RuntimeException("Could not get gfyName for url: " + mGifUrl));
                        }

                        return Observable.just(gfyName);
                    }
                }
            )
            .doOnNext(new Action1<String>() {
                @Override
                public void call(String gfyName) {
                    mGfyName = gfyName;
                }
            });
    }

    private Observable<GfyMetadata> getGfyMetadataObservable() {
        if (mGfyMetadata != null) {
            return Observable.just(mGfyMetadata);
        }

        return getGfyNameObservable()
            .flatMap(new Func1<String, Observable<? extends GfyMetadata>>() {
                @Override
                public Observable<? extends GfyMetadata> call(String gfyName) {
                    return mGfycatService.getMetadata(gfyName);
                }
            })
            .doOnNext(new Action1<GfyMetadata>() {
                @Override
                public void call(GfyMetadata gfyMetadata) {
                    mGfyMetadata = gfyMetadata;
                }
            });
    }

    private Observable<MediaPlayer> getLoadMediaPlayerObservable(Observable<GfyMetadata> gfyMetadataObservable) {
        return Observable.combineLatest(gfyMetadataObservable, mSurfaceTextureSubject,
            new Func2<GfyMetadata, SurfaceTexture, MediaPlayer>() {
                @Override
                public MediaPlayer call(GfyMetadata gfyMetadata, SurfaceTexture surfaceTexture) {
                    if (gfyMetadata == null || surfaceTexture == null) {
                        return null;
                    }

                    MediaPlayer mediaPlayer = new MediaPlayer();

                    mediaPlayer.setLooping(true);

                    mediaPlayer.setOnVideoSizeChangedListener(mAspectRatioListener);

                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mProgressBar.setVisibility(View.GONE);
                            mp.start();
                            mp.seekTo(mCurrentPosition);

                            mVideoProgressBar.setProgress(mCurrentPosition);
                            mVideoProgressBar.setMax(mMediaPlayer.getDuration());

                            // Only set the progress bar visible if the duration is > 1000ms
                            if (mMediaPlayer.getDuration() > 1000) {
                                mVideoProgressBar.setVisibility(View.VISIBLE);
                            }
                            else {
                                mVideoProgressBar.setVisibility(View.INVISIBLE);
                            }

                            if (!mRecordedStats) {
                                Stats stats = new Stats(MainActivity.this);
                                stats.addItem(mGfyMetadata.getGfyItem());
                                mRecordedStats = true;
                            }
                        }
                    });

                    // Enable looping for Samsung devices, tested on Galaxy S5 (4.4.4)
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            mediaPlayer.pause();
                            mediaPlayer.seekTo(0);
                            mediaPlayer.start();
                        }
                    });

                    try {

                        mediaPlayer.setDataSource(gfyMetadata.getGfyItem().getWebmUrl());
                        mediaPlayer.setSurface(new Surface(mVideoView.getSurfaceTexture()));
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    return mediaPlayer;
                }
            }
        )
            .filter(new Func1<MediaPlayer, Boolean>() {
                @Override
                public Boolean call(MediaPlayer mediaPlayer) {
                    return mediaPlayer != null;
                }
            })
            .doOnNext(new Action1<MediaPlayer>() {
                @Override
                public void call(MediaPlayer mediaPlayer) {
                    mMediaPlayer = mediaPlayer;
                }
            });
    }

    private void loadGfy() {
        Observable<GfyMetadata> gfyMetadataObservable = getGfyMetadataObservable();
        Observable<MediaPlayer> readyForDisplayObservable = getLoadMediaPlayerObservable(gfyMetadataObservable);

        mLoadVideoSubscription = AndroidObservable.bindActivity(this, readyForDisplayObservable)
            .subscribeOn(Schedulers.io())
            .subscribe(
                new Action1<MediaPlayer>() {
                    @Override
                    public void call(MediaPlayer mediaPlayer) {
                        try {
                            mediaPlayer.prepareAsync();

                            mVideoProgressBarSubscription = Observable.interval(10, TimeUnit.MILLISECONDS)
                                .filter(new Func1<Long, Boolean>() {
                                    @Override public Boolean call(Long aLong) {
                                        return mMediaPlayer.isPlaying();
                                    }
                                })
                                .map(new Func1<Long, Integer>() {
                                    @Override
                                    public Integer call(Long value) {
                                        return mMediaPlayer.getCurrentPosition();
                                    }
                                })
                                .subscribe(new Action1<Integer>() {
                                               @Override
                                               public void call(Integer progress) {
                                                   mVideoProgressBar.setProgress(progress);
                                               }
                                           },
                                    new Action1<Throwable>() {
                                        @Override public void call(Throwable throwable) {
                                            Crashlytics.logException(throwable);
                                            showErrorDialog();
                                        }
                                    });

                        }
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                },
                new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e("Could not display GIF", throwable);
                        Crashlytics.logException(throwable);
                        showErrorDialog();
                    }
                }
            );
    }

    //////////////////////////////////////////////////////////////////////////
    // Listeners

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurfaceTextureSubject.onNext(surface);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            correctVideoAspectRatio();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mSurfaceTextureSubject.onNext(null);
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private MediaPlayer.OnVideoSizeChangedListener mAspectRatioListener = new MediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int dwidth, int dheight) {
            correctVideoAspectRatio();
        }
    };

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

        // Update the dimensions of the video progress bar
        int horizontalPad = Math.round((vwidth - vwidth * scaleX) / 2f);
        int verticalMargin = Math.round(mVideoRect.height() / 2f - 0.5f);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mVideoProgressBar.getLayoutParams();
        params.topMargin = verticalMargin;
        mVideoProgressBar.requestLayout();

        mVideoProgressBar.setPadding(horizontalPad, 0, horizontalPad, 0);
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
