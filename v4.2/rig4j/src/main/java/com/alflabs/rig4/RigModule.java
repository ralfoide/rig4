package com.alflabs.rig4;

import com.alflabs.rig4.exp.Exp;
import com.alflabs.rig4.exp.GDocReader;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.IClock;
import com.alflabs.utils.ILogger;
import com.alflabs.utils.JavaClock;
import com.alflabs.utils.JavaLogger;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class RigModule {

    @Singleton
    @Provides
    public IClock provideClock() {
        return new JavaClock();
    }

    @Singleton
    @Provides
    public FileOps provideFileOps() {
        return new FileOps();
    }

    @Singleton
    @Provides
    public ILogger provideILogger() {
        return new JavaLogger();
    }

    @Singleton
    @Provides
    public GDocReader provideGDocReader(Flags flags, ILogger logger) {
        return new GDocReader(flags, logger);
    }
}
