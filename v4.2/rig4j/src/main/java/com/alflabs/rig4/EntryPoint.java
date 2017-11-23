package com.alflabs.rig4;

import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.ILogger;
import com.alflabs.utils.StringUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;

public class EntryPoint {
    private static final String TAG = EntryPoint.class.getSimpleName();

    private static final String FLAG_HELP = "help";
    private static final String FLAG_CONFIG = "config";

    public static void main(String[] args) {
        final IRigComponent component = DaggerIRigComponent
                .builder()
                .rigModule(new RigModule())
                .build();

        System.out.println("Hello World!");

        final ILogger logger = component.getLogger();
        final Flags flags = component.getFlags();
        component.getExp().declareFlags();
        component.getTemplater().declareFlags();
        component.getBlobStore().declareFlags();
        component.getGDocReader().declareFlags();

        flags.addBool(FLAG_HELP, false, "Displays help");
        flags.addString(FLAG_CONFIG, "~/.rig42rc", "Config file path");

        if (!flags.parseCommandLine(args)
                || !flags.parseConfigFile(StringUtils.expandUserHome(flags.getString(FLAG_CONFIG)))
                || flags.getBool(FLAG_HELP)) {
            flags.usage();
            System.exit(1);
        }

        try {
            component.getGDocReader().init();
            component.getExp().start();
            logger.d(TAG, "Done.");
        } catch (GeneralSecurityException | IOException | URISyntaxException | IllegalAccessException | InvocationTargetException e) {
            logger.d(TAG, "Failure", e);
        }
    }
}
