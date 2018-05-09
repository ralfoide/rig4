package com.alflabs.rig4.struct;

import com.alflabs.annotations.NonNull;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class Index {
    public static Index create(@NonNull List<ArticleEntry> articleEntries, @NonNull List<BlogEntry> blogEntries) {
        return new AutoValue_Index(articleEntries, blogEntries);
    }

    public abstract List<ArticleEntry> getArticleEntries();
    public abstract List<BlogEntry> getBlogEntries();
}
