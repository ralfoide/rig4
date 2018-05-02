package com.alflabs.rig4.exp;

import com.alflabs.annotations.NonNull;
import com.alflabs.rig4.Timing;
import com.alflabs.rig4.flags.Flags;
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
            @NonNull BaseData data,
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

    public interface TemplateProvider {
        String getTemplate(Flags flags) throws IOException;
    }

    public static abstract class BaseData implements TemplateProvider {
        private final String mCss;
        private final String mGAUid;
        private final String mPageTitle;
        private final String mPageFilename;
        private final String mSiteTitle;
        private final String mSiteBaseUrl;
        private final String mBannerFilename;

        // Callers should use derived classes: ArticleData.create(), etc.
        private BaseData(
                String css,
                String GAUid,
                String pageTitle,
                String pageFilename,
                String siteTitle,
                String siteBaseUrl,
                String bannerFilename) {
            mCss = css;
            mGAUid = GAUid;
            mPageTitle = pageTitle;
            mPageFilename = pageFilename;
            mSiteTitle = siteTitle;
            mSiteBaseUrl = siteBaseUrl;
            mBannerFilename = bannerFilename;
        }

        public String getCss() {
            return mCss;
        }

        public String getGAUid() {
            return mGAUid;
        }

        public String getPageTitle() {
            return mPageTitle;
        }

        public String getPageFilename() {
            return mPageFilename;
        }

        public String getSiteTitle() {
            return mSiteTitle;
        }

        public String getSiteBaseUrl() {
            return mSiteBaseUrl;
        }

        public String getBannerFilename() {
            return mBannerFilename;
        }
    }

    public static class ArticleData extends BaseData {
        private final String mContent;

        private ArticleData(
                String css,
                String GAUid,
                String pageTitle,
                String pageFilename,
                String siteTitle,
                String siteBaseUrl,
                String bannerFilename,
                String content) {
            super(  css,
                    GAUid,
                    pageTitle,
                    pageFilename,
                    siteTitle,
                    siteBaseUrl,
                    bannerFilename);
            mContent = content;
        }

        public static ArticleData create(
                String css,
                String GAUid,
                String pageTitle,
                String pageFilename,
                String siteTitle,
                String siteBaseUrl,
                String bannerFilename,
                String content) {
            return new ArticleData(
                    css,
                    GAUid,
                    pageTitle,
                    pageFilename,
                    siteTitle,
                    siteBaseUrl,
                    bannerFilename,
                    content);
        }

        @Override
        public String getTemplate(Flags flags) throws IOException {
            return Resources.toString(
                    Resources.getResource(this.getClass(), flags.getString(EXP_TEMPLATE_ARTICLE)),
                    Charsets.UTF_8);
        }

        public String getContent() {
            return mContent;
        }
    }

    public static class BlogPageData extends ArticleData {
        private final String mBlogHeader;
        private final String mPostDate;
        private final String mPostTitle;

        private BlogPageData(
                String css,
                String GAUid,
                String pageTitle,
                String pageFilename,
                String siteTitle,
                String siteBaseUrl,
                String bannerFilename,
                String content,
                String blogHeader,
                String postDate,
                String postTitle) {
            super(  css,
                    GAUid,
                    pageTitle,
                    pageFilename,
                    siteTitle,
                    siteBaseUrl,
                    bannerFilename,
                    content);
            mBlogHeader = blogHeader;
            mPostDate = postDate;
            mPostTitle = postTitle;
        }

        public static BlogPageData create(
                String css,
                String GAUid,
                String pageTitle,
                String pageFilename,
                String siteTitle,
                String siteBaseUrl,
                String bannerFilename,
                String content,
                String blogHeader,
                String postDate,
                String postTitle) {
            return new BlogPageData(
                    css,
                    GAUid,
                    pageTitle,
                    pageFilename,
                    siteTitle,
                    siteBaseUrl,
                    bannerFilename,
                    content,
                    blogHeader,
                    postDate,
                    postTitle);
        }

        @Override
        public String getTemplate(Flags flags) throws IOException {
            return Resources.toString(
                    Resources.getResource(this.getClass(), flags.getString(EXP_TEMPLATE_BLOG_PAGE)),
                    Charsets.UTF_8);
        }

        public String getBlogHeader() {
            return mBlogHeader;
        }

        public String getPostDate() {
            return mPostDate;
        }

        public String getPostTitle() {
            return mPostTitle;
        }
    }

    public static class BlogPostData extends BlogPageData {
        private final String mPostExtraLink;

        private BlogPostData(
                String siteBaseUrl,
                String content,
                String postDate,
                String postTitle,
                String postExtraLink) {
            super(  "",
                    "",
                    "",
                    "",
                    "",
                    siteBaseUrl,
                    "",
                    content,
                    "",
                    postDate,
                    postTitle);
            mPostExtraLink = postExtraLink;
        }

        public static BlogPostData create(
                String siteBaseUrl,
                String content,
                String postDate,
                String postTitle,
                String postExtraLink) {
            return new BlogPostData(
                    siteBaseUrl,
                    content,
                    postDate,
                    postTitle,
                    postExtraLink);
        }

        @Override
        public String getTemplate(Flags flags) throws IOException {
            return Resources.toString(
                    Resources.getResource(this.getClass(), flags.getString(EXP_TEMPLATE_BLOG_POST)),
                    Charsets.UTF_8);
        }

        public String getPostExtraLink() {
            return mPostExtraLink;
        }
    }
}
