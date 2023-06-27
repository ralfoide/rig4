package com.alflabs.rig4k.dagger

import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class JsonFactoryModule {

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
