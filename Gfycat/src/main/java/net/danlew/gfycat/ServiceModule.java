package net.danlew.gfycat;

import dagger.Module;
import dagger.Provides;
import net.danlew.gfycat.service.GfycatService;
import net.danlew.gfycat.ui.MainActivity;

import javax.inject.Singleton;

@Module(
    injects = MainActivity.class
)
public class ServiceModule {

    @Provides
    @Singleton
    GfycatService provideGfycatService() {
        return new GfycatService();
    }
}
