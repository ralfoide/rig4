package com.alflabs.rig4;

import dagger.Component;

@Component(modules = { RigModule.class })
public interface IRigComponent {

    @Component.Builder
    interface Builder {
        IRigComponent build();
        Builder rigModule(RigModule rigModule);
    }
}
