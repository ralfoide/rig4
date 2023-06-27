package com.alflabs.rig4k.dagger

import com.alflabs.utils.FileOps
import com.alflabs.utils.IClock
import com.alflabs.utils.ILogger
import com.alflabs.utils.JavaClock
import com.alflabs.utils.JavaLogger
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RigModule {
    @Singleton
    @Provides
    fun provideClock(): IClock {
        return JavaClock()
    }

    @Singleton
    @Provides
    fun provideFileOps(): FileOps {
        return FileOps()
    }

    @Singleton
    @Provides
    fun provideILogger(): ILogger {
        return JavaLogger()
    }

    //    @Singleton
    //    @Provides
    //    public GDocReader provideGDocReader(JsonFactory jsonFactory, Flags flags, Timing timing, ILogger logger) {
    //        return new GDocReader(jsonFactory, flags, timing, logger);
    //    }
    @Singleton
    @Provides
    fun provideJsonFactory(): JsonFactory {
        return JacksonFactory.getDefaultInstance()
    }
}
