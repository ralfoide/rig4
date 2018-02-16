package com.alflabs.rig4.exp;

import com.alflabs.annotations.NonNull;
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
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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

    public String generate(TemplateData data) throws IOException, InvocationTargetException, IllegalAccessException, ParseException {
        String source = getTemplate();
        Map<String, String> vars = new TreeMap<>();

        return generateImpl(data, source, vars);
    }

    private String generateImpl(TemplateData data, String source, Map<String, String> vars) throws ParseException, InvocationTargetException, IllegalAccessException {
        StringBuilder result = new StringBuilder();
        String sourceLower = source.toLowerCase(Locale.US);
        int len = source.length();
        for (int offset = 0; offset < len; ) {
            int start = source.indexOf("{{", offset);
            int end = source.indexOf("}}", offset);
            if (start == -1 || end <= start) {
                result.append(source.substring(offset, len));
                break;
            }
            if (start > offset) {
                result.append(source.substring(offset, start));
            }
            offset = end + 2;

            String command = source.substring(start + 2, end);
            int dot = command.indexOf('.');
            if (dot == -1) {
                throw new ParseException("Invalid command '" + command + "' in template", start);
            }

            String name = command.substring(dot + 1).toLowerCase(Locale.US);
            String value = getVarValue(data, name, vars);
            String function = dot <= 0 ? "" : command.substring(0, dot).toLowerCase(Locale.US);

            String replacement = value;
            switch (function) {
            case "if":
                int endif = sourceLower.indexOf("{{endif}}", offset);
                if (endif < offset) {
                    throw new ParseException("Missing '{{EndIf}}' for '" + command + "' in template", offset);
                }
                if (value.trim().isEmpty()) {
                    replacement = "";
                } else {
                    String innerSource = source.substring(offset, endif);
                    replacement = generateImpl(data, innerSource, vars);
                }
                offset = endif + "{{endif}}".length();
                break;
            case "":
                break;
            default:
                throw new ParseException("Invalid function in '" + command + "' in template", start);
            }

            result.append(replacement);
        }

        return result.toString();
    }

    @NonNull
    private String getVarValue(
            @NonNull TemplateData data,
            @NonNull String name,
            @NonNull Map<String, String> vars) throws InvocationTargetException, IllegalAccessException {
        String value = vars.get(name);
        if (value != null) {
            return value;
        }

        for (Method method : data.getClass().getMethods()) {
            String mname = method.getName();
            if (!mname.toLowerCase(Locale.US).equals("get" + name)) {
                continue;
            }
            if ((method.getModifiers() & Modifier.PUBLIC) == 0
                    || !String.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            value = (String) method.invoke(data);
            break;
        }

        if (value == null) {
            value = "";
        }
        vars.put(name, value);

        return value;
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
