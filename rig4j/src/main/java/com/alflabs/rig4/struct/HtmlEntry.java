package com.alflabs.rig4.struct;

import com.alflabs.annotations.NonNull;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class HtmlEntry {
    public static HtmlEntry create(@NonNull String fileId, @NonNull String destName) {
        return new AutoValue_HtmlEntry(fileId, destName);
    }

    public abstract String getFileId();
    public abstract String getDestName();
}
