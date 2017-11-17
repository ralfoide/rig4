package com.alflabs.rig4;

import com.alflabs.rig4.flags.Flags;

public class EntryPoint {
    public static void main(String[] args) {

        IRigComponent component = DaggerIRigComponent
                .builder()
                .rigModule(new RigModule())
                .build();

        System.out.println("Hello World!");

        final Flags flags = component.getFlags();

        flags.addBool("help", false, "Displays help");
        flags.addString("config", "~/.rig4rc", "Config file path");

        if (!flags.parseCommandLine(args) || !flags.parseConfigFile(flags.getString("config"))) {
            flags.usage();
            System.exit(1);
        }
    }
}
