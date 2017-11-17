package com.alflabs.rig4.flags;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;

public abstract class Flag<T> {

    private final String mName;
    private final String mDescription;
    private final T mDefaultValue;
    private T mValue;

    private Flag(@NonNull String name, @NonNull T defaultValue, @Null String description) {
        mName = name;
        mDefaultValue = defaultValue;
        mDescription = description;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    @Null
    public String getDescription() {
        return mDescription;
    }

    public abstract void setValue(@NonNull String value);

    protected void __setValue(@NonNull T value) {
        mValue = value;
    }

    @NonNull
    public T getValue() {
        if (mValue == null) {
            return mDefaultValue;
        }
        return mValue;
    }

    /** Indicates if a value has been set or if the default value would be used. */
    public boolean isDefaultValue() {
        // This uses an object-reference check on purpose and must NOT be changed
        // to an equals() check.
        return mValue == null || mValue == mDefaultValue;
    }

    /**
     * Indicates where the parser should allow no extra parameter value.
     * This is used by the Boolean flag to avoid parsing an extra parameter
     * and indicates what should be the default value when not specified.
     */
    public String getOptionalParameter() {
        return null;
    }

    public static class String_ extends Flag<String> {
        public String_(@NonNull String name, @NonNull String defaultValue, @Null String description) {
            super(name, defaultValue, description);
        }

        @Override
        public void setValue(@NonNull String value) {
            __setValue(value);
        }
    }

    public static class Int extends Flag<Integer> {
        public Int(@NonNull String name, int defaultValue, @Null String description) {
            super(name, defaultValue, description);
        }

        @Override
        public void setValue(@NonNull String value) throws NumberFormatException {
            __setValue(Integer.parseInt(value));
        }
    }

    public static class Bool extends Flag<Boolean> {
        public Bool(@NonNull String name, boolean defaultValue, @Null String description) {
            super(name, defaultValue, description);
        }

        @Override
        public void setValue(@NonNull String value) {
            __setValue(Boolean.parseBoolean(value));
        }

        @Override
        public String getOptionalParameter() {
            return Boolean.TRUE.toString();
        }
    }
}
