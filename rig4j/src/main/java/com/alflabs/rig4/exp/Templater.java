package com.alflabs.rig4.exp;

import com.alflabs.annotations.NonNull;
import com.alflabs.rig4.Timing;
import com.alflabs.rig4.flags.Flags;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Resources;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Singleton
public class Templater {
    private static final String EXP_TEMPLATE_ARTICLE   = "exp-template-article";
    private static final String EXP_TEMPLATE_BLOG_PAGE = "exp-template-blog-page";
    private static final String EXP_TEMPLATE_BLOG_POST = "exp-template-blog-post";

    private final Flags mFlags;
    private final Timing.TimeAccumulator mTiming;

    private Map<Class<?>, String> mTemplates = new HashMap<>();

    @Inject
    public Templater(Flags flags, Timing timing) {
        mFlags = flags;
        mTiming = timing.get("Templater");
    }

    public Templater(Flags flags, Timing timing, String template) {
        this(flags, timing);
        mTemplates.put(null, template);  // set the default fallback template
    }

    public void declareFlags() {
        mFlags.addString(EXP_TEMPLATE_ARTICLE, "article.html", "Exp Template Article");
        mFlags.addString(EXP_TEMPLATE_BLOG_PAGE, "blog_page.html", "Exp Template Blog Page");
        mFlags.addString(EXP_TEMPLATE_BLOG_POST, "blog_post.html", "Exp Template Blog Post Fragment");
    }

    private String getTemplate(TemplateProvider data) throws IOException {
        // Find a template already loaded for this data class.
        Class<?> clazz = data.getClass();
        String template = mTemplates.get(clazz);
        if (template != null) {
            return template;
        }

        // Use the default fallback template if defined.
        template = mTemplates.get(null);
        if (template != null) {
            return template;
        }

        // Get the template from the TemplateProvider and cache it.
        template = data.getTemplate(mFlags);
        Preconditions.checkNotNull(template);
        mTemplates.put(clazz, template);
        return template;
    }

    public String generate(BaseData data) throws IOException, InvocationTargetException, IllegalAccessException, ParseException {
        mTiming.start();
        try {
            String source = getTemplate(data);
            Map<String, String> vars = new TreeMap<>();

            return generateImpl(data, source, vars);
        } finally {
            mTiming.end();
        }
    }

    private String generateImpl(BaseData data, String source, Map<String, String> vars) throws ParseException, InvocationTargetException, IllegalAccessException {
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
            String function = dot <= 0 ? "" : command.substring(0, dot).toLowerCase(Locale.US);

            String replacement = "";
            boolean negate = false;
            switch (function) {
            case "if!":
                negate = true;
                // explicit break-through
            case "if":
                int endif = sourceLower.indexOf("{{endif}}", offset);
                if (endif < offset) {
                    throw new ParseException("Missing '{{EndIf}}' for '" + command + "' in template", offset);
                }

                boolean isEq = false;
                boolean isNeq = false;
                String value1 = "";
                String value2 = "";
                int pos = name.indexOf("==");
                if (pos != -1) {
                    isEq = true;
                } else {
                    pos = name.indexOf("!=");
                    isNeq = pos != -1;
                }
                if (isEq || isNeq) {
                    String name1 = name.substring(0, pos).trim();
                    value1 = getVarValue(data, name1, vars);

                    value2 = name.substring(pos + 2).trim();
                    if (value2.startsWith(".")) {
                        value2 = getVarValue(data, value2.substring(1), vars);
                    }
                } else {
                    value1 = getVarValue(data, name, vars);
                }

                boolean useInnerSource = false;
                if (isEq && value1.equals(value2)) {
                    useInnerSource = true;
                } else if (isNeq && !value1.equals(value2)) {
                    useInnerSource = true;
                } else if (!isEq && !isNeq && !value1.trim().isEmpty()) {
                    useInnerSource = true;
                }
                if (negate) {
                    useInnerSource = !useInnerSource;
                }

                if (useInnerSource) {
                    String innerSource = source.substring(offset, endif);
                    replacement = generateImpl(data, innerSource, vars);
                }

                offset = endif + "{{endif}}".length();
                break;
            case "":
                // Straight replacement e.g. {{.Var}} ==> value of Var
                replacement = getVarValue(data, name, vars);
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
            @NonNull BaseData data,
            @NonNull String name,
            @NonNull Map<String, String> vars) throws InvocationTargetException, IllegalAccessException {
        name = name.trim();
        if (name.isEmpty()) {
            return null;
        }

        String value = vars.get(name);
        if (value != null) {
            return value;
        }

        // Find the value using a getter method, if any is available
        String target = "get" + name;
        Class<?> clazz = data.getClass();
        for (Method method : clazz.getMethods()) {
            String mname = method.getName();
            if (!mname.toLowerCase(Locale.US).equals(target)) {
                continue;
            }
            if ((method.getModifiers() & Modifier.PUBLIC) == 0
                    || !String.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            value = (String) method.invoke(data);
            break;
        }

        // Otherwise look for the value using an internal public field
        target = "m" + name;
        clazzLoop: while (clazz != null) {
            for (Field field : clazz.getFields()) {
                String fname = field.getName();
                if (!fname.toLowerCase(Locale.US).equals(target)) {
                    continue;
                }
                if ((field.getModifiers() & Modifier.PUBLIC) == 0
                        || !String.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                value = (String) field.get(data);
                break clazzLoop;
            }
            clazz = clazz.getSuperclass();
        }

        if (value == null) {
            value = "";
        }
        vars.put(name, value);

        return value;
    }

    public interface TemplateProvider {
        @NonNull String getTemplate(Flags flags) throws IOException;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static abstract class BaseData implements TemplateProvider {
        public final String mCss;
        public final String mGAUid;
        public final String mPageTitle;
        public final String mRelPageLink;
        public final String mSiteTitle;
        public final String mAbsSiteLink;
        public final String mRelSiteLink;
        public final String mRelBannerLink;

        // Callers should use derived classes: ArticleData.create(), etc.
        @VisibleForTesting
        BaseData(
                String siteTitle,
                String absSiteLink,
                String relSiteLink,
                String css,
                String GAUid,
                String pageTitle,
                String relPageLink,
                String relBannerLink) {
            mCss = css;
            mGAUid = GAUid;
            mPageTitle = pageTitle;
            mRelPageLink = relPageLink;
            mSiteTitle = siteTitle;
            mAbsSiteLink = absSiteLink;
            mRelSiteLink = relSiteLink;
            mRelBannerLink = relBannerLink;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class ArticleData extends BaseData {
        public final String mContent;
        public final String mRelImageLink;

        public ArticleData(
                String siteTitle,
                String absSiteLink,
                String relSiteLink,
                String relBannerLink,
                String css,
                String GAUid,
                String pageTitle,
                String relPageLink,
                String content,
                String relImageLink) {
            super(siteTitle,
                    absSiteLink,
                    relSiteLink,
                    css,
                    GAUid,
                    pageTitle,
                    relPageLink,
                    relBannerLink);
            mContent = content;
            mRelImageLink = relImageLink;
        }

        @NonNull
        @Override
        public String getTemplate(Flags flags) throws IOException {
            return Resources.toString(
                    Resources.getResource(this.getClass(), flags.getString(EXP_TEMPLATE_ARTICLE)),
                    Charsets.UTF_8);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class BlogPageData extends ArticleData {
        public final String mRelPrevPageLink;
        public final String mRelNextPageLink;
        public final String mBlogHeader;
        public final String mPostDate;
        public final String mPostTitle;
        public final String mPostCategory;
        public final String mRelPostCatLink;
        public final String mGenInfo;

        public BlogPageData(
                String siteTitle,
                String absSiteLink,
                String relSiteLink,
                String relBannerLink,
                String css,
                String GAUid,
                String pageTitle,
                String relPageLink,
                String relPrevPageLink,
                String relNextPageLink,
                String blogHeader,
                String postTitle,
                String postDate,
                String postCategory,
                String relPostCatLink,
                String content,
                String genInfo,
                String relImageLink) {
            super(siteTitle,
                    absSiteLink,
                    relSiteLink,
                    relBannerLink,
                    css,
                    GAUid,
                    pageTitle,
                    relPageLink,
                    content,
                    relImageLink);
            mRelPrevPageLink = relPrevPageLink;
            mRelNextPageLink = relNextPageLink;
            mBlogHeader = blogHeader;
            mPostDate = postDate;
            mPostTitle = postTitle;
            mPostCategory = postCategory;
            mRelPostCatLink = relPostCatLink;
            mGenInfo = genInfo;
        }

        @NonNull
        @Override
        public String getTemplate(Flags flags) throws IOException {
            return Resources.toString(
                    Resources.getResource(this.getClass(), flags.getString(EXP_TEMPLATE_BLOG_PAGE)),
                    Charsets.UTF_8);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class BlogPostData extends BlogPageData {
        public final String mRelPostFullLink;
        public final String mRelPostExtraLink;

        public BlogPostData(
                String absSiteLink,
                String relSiteLink,
                String postTitle,
                String postDate,
                String postCategory,
                String relPostCatLink,
                String relPostFullLink,
                String relPostExtraLink,
                String content) {
            super(  "",
                    absSiteLink,
                    relSiteLink,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    postTitle,
                    postDate,
                    postCategory,
                    relPostCatLink,
                    content,
                    "",
                    "");
            mRelPostFullLink = relPostFullLink;
            mRelPostExtraLink = relPostExtraLink;
        }

        @NonNull
        @Override
        public String getTemplate(Flags flags) throws IOException {
            return Resources.toString(
                    Resources.getResource(this.getClass(), flags.getString(EXP_TEMPLATE_BLOG_POST)),
                    Charsets.UTF_8);
        }
    }
}
