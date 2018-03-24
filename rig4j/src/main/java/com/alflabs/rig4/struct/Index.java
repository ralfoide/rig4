package com.alflabs.rig4.struct;

import com.alflabs.annotations.NonNull;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class Index {
    public static Index create(@NonNull List<HtmlEntry> htmlEntries, @NonNull List<String> blogIds) {
        return new AutoValue_Index(htmlEntries, blogIds);
    }

    public abstract List<HtmlEntry> getHtmlEntries();
    public abstract List<String> getBlogIds();
}
