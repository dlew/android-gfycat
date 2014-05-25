package net.danlew.gfycat;

import android.app.Application;
import android.content.Context;
import dagger.ObjectGraph;

public class GfycatApplication extends Application {

    private ObjectGraph mObjectGraph;

    @Override
    public void onCreate() {
        super.onCreate();

        mObjectGraph = ObjectGraph.create(new ServiceModule());
    }

    public void inject(Object o) {
        mObjectGraph.inject(o);
    }

    public static GfycatApplication get(Context context) {
        return (GfycatApplication) context.getApplicationContext();
    }
}
