package com.alflabs.rig4.exp;

import com.alflabs.rig4.flags.Flags;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class ExpFlags {
    public static final String EXP_DOC_ID = "exp-doc-id";
    public static final String EXP_DEST_DIR = "exp-dest-dir";
    public static final String EXP_GA_UID = "exp-ga-uid";
    public static final String EXP_SITE_TITLE = "exp-site-title";
    public static final String EXP_SITE_BANNER = "exp-site-banner";
    /** Base URL is expected to be in the format http(s)://some.host(/folder)/ with trailing slash. */
    public static final String EXP_SITE_BASE_URL = "exp-site-base-url";

    private final Flags mFlags;

    @Inject
    public ExpFlags(Flags flags) {
        mFlags = flags;
    }

    public void declareFlags() {
        mFlags.addString(EXP_DOC_ID,        "",           "Exp gdoc id");
        mFlags.addString(EXP_DEST_DIR,      "",           "Exp dest dir");
        mFlags.addString(EXP_GA_UID,        "",           "Exp GA UID");
        mFlags.addString(EXP_SITE_TITLE,    "Site Title", "Web site title");
        mFlags.addString(EXP_SITE_BANNER,   "header.jpg", "Web site banner filename");
        mFlags.addString(EXP_SITE_BASE_URL, "http://localhost/folder/", "Web site base URL");
    }
}
