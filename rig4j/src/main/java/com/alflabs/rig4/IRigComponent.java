package com.alflabs.rig4;

import com.alflabs.rig4.blog.BlogFlags;
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
    Flags getFlags();
    ExpFlags getExpFlags();
    BlogFlags getBlogFlags();
    GDocReader getGDocReader();
    BlobStore getBlobStore();
    Templater getTemplater();
    ILogger getLogger();

    @Component.Builder
    interface Builder {
        IRigComponent build();
        Builder rigModule(RigModule rigModule);
    }
}
