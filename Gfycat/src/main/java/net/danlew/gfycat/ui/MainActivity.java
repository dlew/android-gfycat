package net.danlew.gfycat.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ProgressBar;
import butterknife.ButterKnife;
import butterknife.InjectView;
import net.danlew.gfycat.GfycatApplication;
import net.danlew.gfycat.R;
import net.danlew.gfycat.model.UrlCheck;
import net.danlew.gfycat.service.GfycatService;
import rx.Observable;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;
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
            String url = getIntent().getData().toString();
            Observable<UrlCheck> checkUrlObservable = mGfycatService.checkUrl(url);

            mCompositeSubscription.add(
                AndroidObservable.bindActivity(this, checkUrlObservable)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Action1<UrlCheck>() {
                        @Override
                        public void call(UrlCheck urlCheck) {
                            // TODO
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
