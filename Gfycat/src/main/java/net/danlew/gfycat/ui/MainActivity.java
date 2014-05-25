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

import javax.inject.Inject;


public class MainActivity extends Activity {

    @Inject
    GfycatService mGfycatService;

    @InjectView(R.id.progress_bar)
    ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GfycatApplication.get(this).inject(this);

        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        if (savedInstanceState == null) {
            String url = getIntent().getData().toString();
            Observable<UrlCheck> checkUrlObservable = mGfycatService.checkUrl(url);
        }
    }
}
