package com.alflabs.rig4.struct;

import com.alflabs.annotations.NonNull;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class Index {
    public static Index create(@NonNull List<ArticleEntry> articleEntries, @NonNull List<String> blogIds) {
        return new AutoValue_Index(articleEntries, blogIds);
    }

    public abstract List<ArticleEntry> getArticleEntries();
    public abstract List<String> getBlogIds();
}
