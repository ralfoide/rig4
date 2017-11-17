package com.alflabs.rig4.flags;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.ILogger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class Flags {
    private final static String TAG = Flags.class.getSimpleName();

    private final FileOps mFileOps;
    private final ILogger mLogger;
    private Map<String, Flag> mFlagMap = new TreeMap<>();

    public static class NotDefinedException extends RuntimeException {
        public NotDefinedException(String msg) {
            super(msg);
        }
    }

    @Inject
    public Flags(FileOps fileOps, ILogger logger) {
        mFileOps = fileOps;
        mLogger = logger;
    }

    @NonNull
    private Flag getFlag(String name) throws NotDefinedException {
        Flag flag = mFlagMap.get(name);
        if (flag == null) {
            throw new NotDefinedException("Flag '" + name + "' is not defined.");
        }
        return flag;
    }

    @NonNull
    public String getString(String name) throws NotDefinedException {
        return getFlag(name).getValue().toString();
    }

    @NonNull
    public int getInt(String name) throws NotDefinedException {
        return (Integer) (getFlag(name).getValue());
    }

    @NonNull
    public boolean getBool(String name) throws NotDefinedException {
        return (Boolean) (getFlag(name).getValue());
    }

    public Flags addString(@NonNull String name, @NonNull String defaultValue, @Null String description) {
        mFlagMap.put(name, new Flag.String_(name, defaultValue, description));
        return this;
    }

    public Flags addBool(@NonNull String name, @NonNull boolean defaultValue, @Null String description) {
        mFlagMap.put(name, new Flag.Bool(name, defaultValue, description));
        return this;
    }

    public Flags addInt(@NonNull String name, @NonNull int defaultValue, @Null String description) {
        mFlagMap.put(name, new Flag.Int(name, defaultValue, description));
        return this;
    }

    public void usage() {
        int vlen = 0;
        int nlen = 0;
        for (Flag flag : mFlagMap.values()) {
            nlen = Math.max(nlen, flag.getName().length());
            vlen = Math.max(vlen, flag.getValue().toString().length());
        }

        for (Flag flag : mFlagMap.values()) {
            mLogger.d("Usage",
                    String.format("--%-" + nlen + "s: %-" + vlen + "s%s",
                    flag.getName(),
                    flag.getValue(),
                    flag.getDescription() == null ? "" : (", " + flag.getDescription())));
        }
    }

    /**
     * Parses flags from command-line.
     * On error, prints unknown flag on the current logger.
     *
     * (Optional: Consider returning all unmatched arguments that are obviously not flags.)
     *
     * @return True if command-line was empty or consisted of valid flags. False otherwise.
     *  Caller should consider calling {@link #usage()} and exit(1) when false is returned.
     */
    public boolean parseCommandLine(String[] args) {
        boolean error = false;

        if (args == null || args.length == 0) {
            return true;
        }

        // If not ambiguous, accept - + first letter as a shortcut.
        Map<Character, String> shortcuts = new TreeMap<>();
        for (Flag flag : mFlagMap.values()) {
            String name = flag.getName();
            char c = name.charAt(0);
            if (!shortcuts.containsKey(c)) {
                // First time seeing a shortcut with that letter
                shortcuts.put(c, name);
            } else if (shortcuts.get(c).length() > 0) {
                // Second time seeing a shortcut with that letter
                shortcuts.put(c, "");
            }
        }

        Deque<String> stack = new ArrayDeque<>(Arrays.asList(args));
        while (!stack.isEmpty()) {
            String name = stack.pop();
            String value = null;
            Flag flag = null;

            if (name.startsWith("-")) {
                int pos = name.indexOf('=');
                if (pos > 1 && pos < name.length() - 1) {
                    value = name.substring(pos + 1);
                    name = name.substring(0, pos).trim();
                }
            }

            String key = "";

            if (name.startsWith("--")) {
                key = name.substring(2);
            } else if (name.startsWith("-")) {
                key = name.substring(1);
            }

            if (key.length() == 1) {
                key = shortcuts.get(key.charAt(0));
            }

            if (!key.isEmpty()) {
                flag = mFlagMap.get(key);
            }

            if (flag == null) {
                mLogger.d(TAG, "Unknown parameter: " + name);
                error = true;
            } else {
                String optional = flag.getOptionalParameter();

                if (optional != null && value == null) {
                    String next = stack.peek();
                    if (next == null || next.startsWith("-")) {
                        value = optional;
                    }
                }

                if (value == null && !stack.isEmpty()) {
                    value = stack.pop();
                }
                if (value == null) {
                    mLogger.d(TAG, "No value specified for " + flag.getName());
                    error = true;
                    continue;
                }
                try {
                    flag.setValue(value);
                } catch (Exception e) {
                    mLogger.d(TAG, "Invalid value '" + value + "' for flag " + flag.getName(), e);
                    error = true;
                }
            }
        }

        return !error;
    }

    /**
     * Parses flags from config file.
     * <p/>
     * This is designed to be called _after_ parsing the command line -- this allows the command
     * line to specify the config file name yet override values from the config file.
     * <p/>
     * Consequently this does NOT change flags which value is not the default
     * (e.g. defined via the command line).
     *
     * On error, prints unknown flag on the current logger.
     *
     * @return True if config was empty or consisted of valid flags. False otherwise.
     *  Caller should consider calling {@link #usage()} and exit(1) when false is returned.
     */
    public boolean parseConfigFile(@NonNull String configPath) {
        boolean error = false;

        File file = new File(configPath);
        if (!mFileOps.isFile(file)) {
            mLogger.d(TAG, "No config file '" + configPath + "'");
            return true;
        }

        try {
            Properties props = mFileOps.getProperties(file);

            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String name = (String) entry.getKey();
                String value = (String) entry.getValue();

                Flag flag = mFlagMap.get(name);
                if (flag == null) {
                    mLogger.d(TAG, "Unknown parameter '" + name + "' in config file '" + configPath + "'");
                    error = true;
                    continue;
                }

                if (!flag.isDefaultValue()) {
                    // If the value is not the default one, it has been changed by the command
                    // line parser and we ignore the value from the config file. (Note: one might
                    // be tempted to simply call parseConfigFile() before parseCommandLine();
                    // however doing it this way allows for the command line to specify the name
                    // of the config file, yet the command line arguments take precedence).
                    continue;
                }

                try {
                    flag.setValue(value);
                } catch (Exception e) {
                    mLogger.d(TAG, "Invalid value '" + value + "' for flag " + flag.getName(), e);
                    error = true;
                }
            }
        } catch (IOException e) {
            mLogger.d(TAG, "Failed to load properties from '" + configPath+ "'", e);
            error = true;
        }

        return !error;
    }
}
