package com.alflabs.rig4;

import com.alflabs.rig4.exp.Exp;
import com.alflabs.rig4.exp.ExpFlags;
import com.alflabs.rig4.gdoc.GDocReader;
import com.alflabs.rig4.exp.Templater;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.ILogger;
import dagger.Component;

import javax.inject.Singleton;

@Component(modules = { RigModule.class })
@Singleton
public interface IRigComponent {

    Exp getExp();
    ExpFlags getExpFlags();
    Flags getFlags();
    ILogger getLogger();
    GDocReader getGDocReader();
    BlobStore getBlobStore();
    Templater getTemplater();

    @Component.Builder
    interface Builder {
        IRigComponent build();
        Builder rigModule(RigModule rigModule);
    }
}
