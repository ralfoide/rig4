package com.alflabs.rig4.blog;

import com.alflabs.annotations.NonNull;
import com.alflabs.annotations.Null;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class CatFilter {
    private List<Pattern> mPatterns;
    private HashSet<String> mQuickMatch;

    public CatFilter(@Null String commaSeparateListOfRegexp) {
        if (commaSeparateListOfRegexp != null) {
            int pos = commaSeparateListOfRegexp.indexOf('#');
            if (pos > -1) {
                commaSeparateListOfRegexp = commaSeparateListOfRegexp.substring(0, pos);
            }

            for (String entry : commaSeparateListOfRegexp.split(",")) {
                entry = entry.trim();
                if (!entry.isEmpty()) {
                    if (mPatterns == null) { mPatterns = new ArrayList<>(); }
                    mPatterns.add(Pattern.compile(entry));
                }
            }
        }
    }

    public boolean isEmpty() {
        return mPatterns == null || mPatterns.isEmpty();
    }

    public boolean matches(@NonNull String category) {
        if (mPatterns == null) {
            return false;
        }

        category = category.trim().toLowerCase(Locale.US);

        if (category.length() == 0) {
            return false;
        }

        if (mQuickMatch != null) {
            if (mQuickMatch.contains(category)) {
                return true;
            }
        }

        for (Pattern pattern : mPatterns) {
            if (pattern.matcher(category).matches()) {
                if (mQuickMatch == null) { mQuickMatch = new HashSet<>(); }
                mQuickMatch.add(category);
                return true;
            }
        }
        return false;
    }
}
