package net.danlew.gfycat.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import net.danlew.gfycat.GfycatApplication;
import net.danlew.gfycat.R;
import net.danlew.gfycat.model.ConvertGif;
import net.danlew.gfycat.model.UrlCheck;
import net.danlew.gfycat.service.GfycatService;
import rx.Observable;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;


public class MainActivity extends Activity {

    @Inject
    GfycatService mGfycatService;

    @InjectView(R.id.progress_bar)
    ProgressBar mProgressBar;

    private CompositeSubscription mCompositeSubscription = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GfycatApplication.get(this).inject(this);

        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

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
                );

            mCompositeSubscription.add(
                AndroidObservable.bindActivity(this, gfyNameObservable)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String gfyName) {
                            Toast.makeText(MainActivity.this, "gfyName=" + gfyName, Toast.LENGTH_SHORT).show();
                        }
                    })
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mCompositeSubscription.unsubscribe();
    }
}
