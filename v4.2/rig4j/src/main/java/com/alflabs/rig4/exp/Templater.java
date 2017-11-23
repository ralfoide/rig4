package com.alflabs.rig4.exp;

import com.google.auto.value.AutoValue;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Resources;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Templater {

    private String mTemplate;

    @Inject
    public Templater() {
    }

    private String getTemplate() throws IOException {
        if (mTemplate != null) {
            return mTemplate;
        }
        mTemplate = Resources.toString(
                Resources.getResource(this.getClass(), "template.html"),
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
        public static TemplateData create(String css, String bannerFilename, String GAUid, String pageTitle, String siteTitle, String content) {
            return new AutoValue_Templater_TemplateData(css, bannerFilename, GAUid, pageTitle, siteTitle, content);
        }

        public abstract String getCss();
        public abstract String getBannerFilename();
        public abstract String getGAUid();
        public abstract String getPageTitle();
        public abstract String getSiteTitle();
        public abstract String getContent();
    }

}
