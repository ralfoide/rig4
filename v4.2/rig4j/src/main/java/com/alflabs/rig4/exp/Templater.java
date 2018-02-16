package com.alflabs.rig4.exp;

import com.alflabs.rig4.flags.Flags;
import com.google.auto.value.AutoValue;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Resources;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class Templater {
    private static final String EXP_TEMPLATE_NAME = "exp-template-name";

    private final Flags mFlags;
    private String mTemplate;

    @Inject
    public Templater(Flags flags) {
        mFlags = flags;
    }

    public Templater(Flags flags, String template) {
        mFlags = flags;
        mTemplate = template;
    }

    public void declareFlags() {
        mFlags.addString(EXP_TEMPLATE_NAME, "template2.html", "Exp Template Name");
    }

    private String getTemplate() throws IOException {
        if (mTemplate != null) {
            return mTemplate;
        }
        mTemplate = Resources.toString(
                Resources.getResource(this.getClass(), mFlags.getString(EXP_TEMPLATE_NAME)),
                Charsets.UTF_8);
        Preconditions.checkNotNull(mTemplate);
        return mTemplate;
    }

    public String generate(TemplateData data) throws IOException, InvocationTargetException, IllegalAccessException {
        String result = getTemplate();

        for (Method method : data.getClass().getMethods()) {
            String mname = method.getName();
            if (!mname.startsWith("get")) {
                continue;
            }
            mname = mname.substring("get".length());
            if ((method.getModifiers() & Modifier.PUBLIC) == 0
                    || !String.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            String value = (String) method.invoke(data);
            String key = "{{." + mname + "}}";
            result = result.replaceAll(Pattern.quote(key), Matcher.quoteReplacement(value));
        }

        return result;
    }

    @AutoValue
    public abstract static class TemplateData {
        public static TemplateData create(
                String css,
                String GAUid,
                String pageTitle,
                String pageFilename,
                String siteTitle,
                String siteBaseUrl,
                String bannerFilename,
                String content) {
            return new AutoValue_Templater_TemplateData(
                    css,
                    GAUid,
                    pageTitle,
                    pageFilename,
                    siteTitle,
                    siteBaseUrl,
                    bannerFilename,
                    content);
        }

        public abstract String getCss();
        public abstract String getGAUid();
        public abstract String getPageTitle();
        public abstract String getPageFilename();
        public abstract String getSiteTitle();
        public abstract String getSiteBaseUrl();
        public abstract String getBannerFilename();
        public abstract String getContent();
    }

}
