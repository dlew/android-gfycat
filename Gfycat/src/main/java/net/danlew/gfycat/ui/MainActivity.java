package net.danlew.gfycat.ui;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import net.danlew.gfycat.GfycatApplication;
import net.danlew.gfycat.R;
import net.danlew.gfycat.model.ConvertGif;
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

// TODO: Handle rotation
public class MainActivity extends Activity {

    @Inject
    GfycatService mGfycatService;

    @InjectView(R.id.progress_bar)
    ProgressBar mProgressBar;

    @InjectView(R.id.video_view)
    TextureView mVideoView;

    @InjectView(R.id.error_text_view)
    TextView mErrorTextView;

    private Subscription mGetNameSubscription;

    private BehaviorSubject<SurfaceTexture> mSurfaceTextureSubject = BehaviorSubject.create((SurfaceTexture) null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GfycatApplication.get(this).inject(this);

        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        mVideoView.setSurfaceTextureListener(mSurfaceTextureListener);

        if (savedInstanceState == null) {
            final String url = getIntent().getData().toString();
            Observable<String> gfyNameObservable = mGfycatService.checkUrl(url)
                .flatMap(
                    new Func1<UrlCheck, Observable<String>>() {
                        @Override
                        public Observable<String> call(UrlCheck urlCheck) {
                            if (urlCheck.isUrlKnown()) {
                                return Observable.just(urlCheck.getGfyName());
                            }

                            return mGfycatService.convertGif(url).map(new Func1<ConvertGif, String>() {
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
                                return Observable.error(new RuntimeException("Could not get gfyName for url: " + url));
                            }

                            return Observable.just(gfyName);
                        }
                    }
                );

            Observable<MediaPlayer> readyForDisplayObservable = Observable.combineLatest(
                gfyNameObservable,
                mSurfaceTextureSubject,
                new Func2<String, SurfaceTexture, MediaPlayer>() {
                    @Override
                    public MediaPlayer call(String gfyName, SurfaceTexture surfaceTexture) {
                        if (TextUtils.isEmpty(gfyName) || surfaceTexture == null) {
                            return null;
                        }

                        MediaPlayer mediaPlayer = new MediaPlayer();

                        try {
                            mediaPlayer.setDataSource("http://zippy.gfycat.com/" + gfyName + ".webm");
                            mediaPlayer.setSurface(new Surface(mVideoView.getSurfaceTexture()));
                        }
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        return mediaPlayer;
                    }
                }
            ).filter(new Func1<MediaPlayer, Boolean>() {
                @Override
                public Boolean call(MediaPlayer mediaPlayer) {
                    return mediaPlayer != null;
                }
            });

            mGetNameSubscription = AndroidObservable.bindActivity(this, readyForDisplayObservable)
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<MediaPlayer>() {
                               @Override
                               public void call(MediaPlayer mediaPlayer) {
                                   try {
                                       mediaPlayer.setLooping(true);
                                       mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                           @Override
                                           public void onPrepared(MediaPlayer mp) {
                                               mProgressBar.setVisibility(View.GONE);
                                               mp.start();
                                           }
                                       });

                                       mediaPlayer.prepareAsync();
                                   }
                                   catch (Exception e) {
                                       throw new RuntimeException(e);
                                   }
                               }
                           },
                    new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            mProgressBar.setVisibility(View.GONE);
                            mErrorTextView.setVisibility(View.VISIBLE);
                        }
                    }
                );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mGetNameSubscription.unsubscribe();
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurfaceTextureSubject.onNext(surface);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

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
}
