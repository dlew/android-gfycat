package net.danlew.gfycat;

import android.app.Application;
import android.content.Context;
import com.crashlytics.android.Crashlytics;
import dagger.ObjectGraph;

public class GfycatApplication extends Application {

    public static final String TAG = "Gfycat";

    private ObjectGraph mObjectGraph;

    @Override
    public void onCreate() {
        super.onCreate();

        Crashlytics.start(this);

        Log.configure(TAG, BuildConfig.DEBUG);

        mObjectGraph = ObjectGraph.create(new ServiceModule());
    }

    public void inject(Object o) {
        mObjectGraph.inject(o);
    }

    public static GfycatApplication get(Context context) {
        return (GfycatApplication) context.getApplicationContext();
    }
}
