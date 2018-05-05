package com.alflabs.rig4;

import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.ILogger;
import com.alflabs.utils.StringUtils;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import java.io.IOException;

public class EntryPoint {
    private static final String TAG = EntryPoint.class.getSimpleName();

    private static final String FLAG_HELP = "help";
    private static final String FLAG_CONFIG = "config";
    private static final String FLAG_VERSION = "version";

    public static void main(String[] args) {
        final IRigComponent component = DaggerIRigComponent
                .builder()
                .rigModule(new RigModule())
                .build();

        final ILogger logger = component.getLogger();
        final Flags flags = component.getFlags();
        component.getExp().declareFlags();
        component.getTemplater().declareFlags();
        component.getBlobStore().declareFlags();
        component.getGDocReader().declareFlags();

        flags.addBool(FLAG_HELP, false, "Displays help and exits");
        flags.addBool(FLAG_VERSION, false, "Display the version and exits");
        flags.addString(FLAG_CONFIG, "~/.rig42rc", "Config file path");

        if (!flags.parseCommandLine(args)
                || !flags.parseConfigFile(StringUtils.expandUserHome(flags.getString(FLAG_CONFIG)))
                || flags.getBool(FLAG_HELP)) {
            flags.usage();
            System.exit(flags.getBool(FLAG_HELP) ? 0 : 1);
        }

        try {
            if (flags.getBool(FLAG_VERSION)) {
                System.out.println(getVersion());
                System.exit(0);
            }

            System.out.println("Hello World!");
            System.out.println("Rig4j " + getVersion());

            component.getGDocReader().init();
            component.getExp().start();
            logger.d(TAG, "Done.");
        } catch (Exception e) {
            logger.d(TAG, "Failure", e);
            e.printStackTrace();
        }
    }

    public static String getVersion() throws IOException {
        return Resources.toString(
                    Resources.getResource(EntryPoint.class, "version.txt"),
                    Charsets.UTF_8);
    }
}
