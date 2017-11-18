package com.alflabs.rig4;

import com.alflabs.rig4.exp.Exp;
import com.alflabs.rig4.exp.GDocReader;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.ILogger;
import dagger.Component;

import javax.inject.Singleton;

@Component(modules = { RigModule.class })
@Singleton
public interface IRigComponent {

    Exp getExp();
    Flags getFlags();
    ILogger getLogger();
    GDocReader getGDocReader();

    @Component.Builder
    interface Builder {
        IRigComponent build();
        Builder rigModule(RigModule rigModule);
    }
}
