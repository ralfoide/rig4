package com.alflabs.rig4;

import com.alflabs.rig4.flags.Flags;
import dagger.Component;

import javax.inject.Singleton;

@Component(modules = { RigModule.class })
@Singleton
public interface IRigComponent {

    Flags getFlags();

    @Component.Builder
    interface Builder {
        IRigComponent build();
        Builder rigModule(RigModule rigModule);
    }
}
