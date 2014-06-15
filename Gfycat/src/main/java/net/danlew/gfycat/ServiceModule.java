package net.danlew.gfycat;

import android.app.Application;
import dagger.Module;
import dagger.Provides;
import net.danlew.gfycat.service.GfycatService;
import net.danlew.gfycat.ui.MainActivity;

import javax.inject.Singleton;

@Module(
    injects = MainActivity.class
)
public class ServiceModule {

    private Application mApplication;

    public ServiceModule(Application application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    GfycatService provideGfycatService() {
        return new GfycatService(mApplication);
    }
}
