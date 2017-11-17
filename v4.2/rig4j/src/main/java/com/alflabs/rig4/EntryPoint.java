package com.alflabs.rig4;

import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.StringUtils;

public class EntryPoint {
    private static final String FLAG_HELP = "help";
    private static final String FLAG_CONFIG = "config";

    public static void main(String[] args) {

        IRigComponent component = DaggerIRigComponent
                .builder()
                .rigModule(new RigModule())
                .build();

        System.out.println("Hello World!");

        final Flags flags = component.getFlags();

        flags.addBool(FLAG_HELP, false, "Displays help");
        flags.addString(FLAG_CONFIG, "~/.rig4rc", "Config file path");

        if (!flags.parseCommandLine(args)
                || !flags.parseConfigFile(StringUtils.expandUserHome(flags.getString(FLAG_CONFIG)))) {
            flags.usage();
            System.exit(1);
        }
    }
}
