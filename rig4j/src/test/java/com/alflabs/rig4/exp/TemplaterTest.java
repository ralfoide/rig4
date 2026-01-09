package com.alflabs.rig4.exp;

import com.alflabs.rig4.Timing;
import com.alflabs.rig4.flags.Flags;
import com.alflabs.utils.FileOps;
import com.alflabs.utils.ILogger;
import com.alflabs.utils.MockClock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;


public class TemplaterTest {
    public @Rule MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private FileOps mFileOps;
    @Mock private ILogger mLogger;

    private Flags mFlags;
    private Timing mTiming = new Timing(new MockClock(), mLogger);
    private Templater mTemplater;

    @Before
    public void setUp() throws Exception {
        mFlags = new Flags(mFileOps, mLogger);
        mTemplater = new Templater(mFlags, mFileOps, mTiming);
        mTemplater.declareFlags();
    }

    @Test
    public void testVarReplacement() throws Exception {
        String template = "<h2> {{.SiteTitle}} - {{.PageTitle}} </h2>";
        Templater templater = new Templater(mFlags, mTiming, mFileOps, template);
        String generated = templater.generate(new TestTemplateData("A Site Title", "A Page Title"));
        assertThat(generated).isEqualTo("<h2> A Site Title - A Page Title </h2>");
    }

    @Test
    public void testVarReplacement_NullIsEmpty() throws Exception {
        String template = "<h2> {{.SiteTitle}} - {{.PageTitle}} </h2>";
        Templater templater = new Templater(mFlags, mTiming, mFileOps, template);
        String generated = templater.generate(new TestTemplateData(null, ""));
        assertThat(generated).isEqualTo("<h2>  -  </h2>");
    }

    @Test
    public void testIfEmpty() throws Exception {
        String template = "<h2> {{.SiteTitle}} {{If.PageTitle}}- {{.PageTitle}} {{Endif}}</h2>";
        Templater templater = new Templater(mFlags, mTiming, mFileOps, template);

        String generated1 = templater.generate(new TestTemplateData("A Site", "A Page"));
        assertThat(generated1).isEqualTo("<h2> A Site - A Page </h2>");

        String generated2 = templater.generate(new TestTemplateData("A Site", ""));
        assertThat(generated2).isEqualTo("<h2> A Site </h2>");

        String generated3 = templater.generate(new TestTemplateData("A Site", null));
        assertThat(generated3).isEqualTo("<h2> A Site </h2>");
    }

    @Test
    public void testIfNegEmpty() throws Exception {
        String template = "<h2> {{.SiteTitle}} {{If!.PageTitle}}- {{.PageTitle}} {{Endif}}</h2>";
        Templater templater = new Templater(mFlags, mTiming, mFileOps, template);

        String generated1 = templater.generate(new TestTemplateData("A Site", "A Page"));
        assertThat(generated1).isEqualTo("<h2> A Site </h2>");

        String generated2 = templater.generate(new TestTemplateData("A Site", ""));
        assertThat(generated2).isEqualTo("<h2> A Site -  </h2>");

        String generated3 = templater.generate(new TestTemplateData("A Site", null));
        assertThat(generated3).isEqualTo("<h2> A Site -  </h2>");
    }

    @Test
    public void testIfEqual() throws Exception {
        String template = "<h2> {{.SiteTitle}} {{If.PageTitle == .SiteTitle}}eq {{.PageTitle}} {{Endif}}</h2>";
        Templater templater = new Templater(mFlags, mTiming, mFileOps, template);

        String generated1 = templater.generate(new TestTemplateData("A Site", "A Page"));
        assertThat(generated1).isEqualTo("<h2> A Site </h2>");

        String generated2 = templater.generate(new TestTemplateData("A Site", "A Site"));
        assertThat(generated2).isEqualTo("<h2> A Site eq A Site </h2>");
    }

    @Test
    public void testIfNotEqual() throws Exception {
        String template = "<h2> {{.SiteTitle}} {{If.PageTitle != .SiteTitle}}+ {{.PageTitle}} {{Endif}}</h2>";
        Templater templater = new Templater(mFlags, mTiming, mFileOps, template);

        String generated1 = templater.generate(new TestTemplateData("A Site", "A Page"));
        assertThat(generated1).isEqualTo("<h2> A Site + A Page </h2>");

        String generated2 = templater.generate(new TestTemplateData("A Site", "A Site"));
        assertThat(generated2).isEqualTo("<h2> A Site </h2>");
    }

    @Test
    public void testComplexTemplate_SimpleReplacements() throws Exception {
        String template = "" +
                "<!doctype html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "<meta property=\"og:url\"         content=\"{{.AbsSiteLink}}{{.FwdPageLink}}{{.RelPageLink}}\" />\n" +
                "<meta property=\"og:type\"        content=\"article\" />\n" +
                "<meta property=\"og:title\"       content=\"{{.PageTitle}}\" />\n" +
                "<meta property=\"og:description\" content=\"{{.Description}}\" />\n" +
                "\n" +
                "<title>{{.PageTitle}}</title>\n" +
                "{{.Css}}\n" +
                "</head>\n" +
                "        <a href=\"{{.AbsSiteLink}}\">{{.SiteTitle}}</a>\n" +
                "{{.SourceContent}}\n" +
                "    gtag('config', '{{.GAUid}}');\n";

        Templater templater = new Templater(mFlags, mTiming, mFileOps, template);

        String generated = templater.generate(new Templater.ArticleData(
                "Site Title replacement",
                "http://Site URL/replacement/",
                "../../",
                "fwd/",
                "banner_image.jpg",
                "CSS replacement",
                "GA UID replacement",
                "Page Title replacement",
                "page_file.html",
                "SourceContent replacement\n" +
                "Multiple content.",
                "" /* relImageLink */,
                "" /* headDescription */,
                "Rig4j Gen info"));

        assertThat(generated).isEqualTo("" +
                "<!doctype html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "<meta property=\"og:url\"         content=\"http://Site URL/replacement/fwd/page_file.html\" />\n" +
                "<meta property=\"og:type\"        content=\"article\" />\n" +
                "<meta property=\"og:title\"       content=\"Page Title replacement\" />\n" +
                "<meta property=\"og:description\" content=\"\" />\n" +
                "\n" +
                "<title>Page Title replacement</title>\n" +
                "CSS replacement\n" +
                "</head>\n" +
                "        <a href=\"http://Site URL/replacement/\">Site Title replacement</a>\n" +
                "SourceContent replacement\n" +
                "Multiple content.\n" +
                "    gtag('config', 'GA UID replacement');\n");

    }

    @Test
    public void testComplexTemplate_IfEmpty() throws Exception {
        String template = "" +
                "{{If.NonExistent}}<!doctype html>\n" +
                "<html lang=\"en\">\n" +
                "<meta property=\"og:url\"         content=\"{{.AbsSiteLink}}{{.FwdPageLink}}{{.RelPageLink}}\" />\n" +
                "<meta property=\"og:type\"        content=\"article\" />\n" +
                "<head>{{EndIf}}\n" +
                "<meta property=\"og:title\"       content=\"{{.PageTitle}}\" />\n" +
                "{{IF.Description}}<meta property=\"og:description\" content=\"{{.Description}}\" />{{ENDIF}}\n" +
                "{{if.content}}{{.SourceContent}}{{endif}}\n" +
                "{{if.GAUid}}gtag('config', '{{.GAUid}} is null');{{EndIf}}\n";

        Templater templater = new Templater(mFlags, mTiming, mFileOps, template);

        String generated = templater.generate(new Templater.ArticleData(
                "Site Title replacement",
                "http://Site URL/replacement/",
                "../../",
                "fwd/",
                "banner_image.jpg",
                "CSS replacement",
                null,           // If.Var accepts both null and empty strings
                "Page Title replacement",
                "page_file.html",
                "SourceContent replacement\n" +
                "Multiple content.",
                "" /* relImageLink */,
                "" /* headDescription */,
                "Rig4j Gen info"));

        assertThat(generated).isEqualTo("" +
                "\n" +
                "<meta property=\"og:title\"       content=\"Page Title replacement\" />\n" +
                "\n" +
                "SourceContent replacement\n" +
                "Multiple content.\n" +
                "\n");

    }

    @Test
    public void testArticleTemplate() throws Exception {
        Templater.ArticleData data = new Templater.ArticleData(
                "Site Title replacement",
                "http://Site URL/replacement/",
                "./", // rev
                "./", // fwd
                "banner_image.jpg",
                "CSS replacement",
                "GA UID replacement",
                "Page Title replacement",
                "page_file.html",
                "SourceContent replacement first line\n" +
                "SourceContent replacement second line.",
                "" /* relImageLink */,
                "" /* headDescription */,
                "Rig4j Gen info");

        String generated = mTemplater.generate(data);

        assertThat(generated).containsMatch("property=\"og:url\"\\s+content=\"http://Site URL/replacement/./page_file.html\"");
        assertThat(generated).containsMatch("property=\"og:title\"\\s+content=\"Page Title replacement\"");
        // We don't generate FB OG meta-data for these yet
        assertThat(generated).doesNotContain("property=\"og:description\"");
        assertThat(generated).doesNotContain("property=\"og:image\"");

        assertThat(generated).containsMatch("name=\"twitter:title\"\\s+content=\"Page Title replacement\"");
        // We don't generate Twitter meta-data for these yet
        assertThat(generated).doesNotContain("name=\"twitter:description\"");
        assertThat(generated).doesNotContain("name=\"twitter:image\"");

        assertThat(generated).contains("<title>Page Title replacement</title>");
        assertThat(generated).contains("background-image: url(\"./banner_image.jpg\");");
        assertThat(generated).containsMatch("<style type=\"text/css\">[^<]+CSS replacement\\s*</style>");
        assertThat(generated).contains("<a href=\"http://Site URL/replacement/\">Site Title replacement</a>");
        assertThat(generated).contains("gtag('config', 'GA UID replacement');");
        assertThat(generated).containsMatch(">\\s+Content replacement first line\\s+Content replacement second line.\\s+<");
    }

    @Test
    public void testBlogPageTemplate_forIndex() throws Exception {
        Templater.BlogPageData data = new Templater.BlogPageData(
                /* isIndex= */ true,
                "Site Title replacement",
                "http://Site URL/replacement/",
                "../../",
                "blog/cat/",
                "banner_image.jpg",
                "CSS replacement",
                "GA UID replacement",
                "Page Index Title replacement",
                "top_index.html",
                "prev/page",
                "next/page",
                "<div>SourceBlog Index Header as HTML</div>",
                "",                 // no post title for an index
                "",                 // no post date  for an index
                "",                 // no post category for an index
                "",                 // no post cat link for an index
                "page_index.html",
                "Multiple Posts SourceContent replacement",
                "Rig4j Gen info",
                "main_image.jpg",
                "head description");
        String generated = mTemplater.generate(data);

        // --- This part is common with an article page
        assertThat(generated).containsMatch("property=\"og:url\"\\s+content=\"http://Site URL/replacement/blog/cat/page_index.html\"");
        assertThat(generated).containsMatch("property=\"og:title\"\\s+content=\"Page Index Title replacement\"");
        assertThat(generated).containsMatch("property=\"og:description\"\\s+content=\"head description\"");
        assertThat(generated).containsMatch("property=\"og:image\"\\s+content=\"http://Site URL/replacement/blog/cat/main_image.jpg\"");

        assertThat(generated).containsMatch("name=\"twitter:title\"\\s+content=\"Page Index Title replacement\"");
        // We don't generate Twitter meta-data for these yet
        assertThat(generated).containsMatch("name=\"twitter:description\"\\s+content=\"head description\"");
        assertThat(generated).containsMatch("name=\"twitter:image\"\\s+content=\"http://Site URL/replacement/blog/cat/main_image.jpg\"");

        assertThat(generated).contains("<title>Page Index Title replacement</title>");
        assertThat(generated).contains("background-image: url(\"banner_image.jpg\");");
        assertThat(generated).containsMatch("<style type=\"text/css\">[^<]+CSS replacement\\s*</style>");
        assertThat(generated).contains("<a href=\"http://Site URL/replacement/\">Site Title replacement</a>");
        assertThat(generated).contains("gtag('config', 'GA UID replacement');");

        // --- This part is specific to a blog index page
        assertThat(generated).contains("<div>SourceBlog Index Header as HTML</div>");
        assertThat(generated).doesNotContain("post-cat-text");
        assertThat(generated).containsMatch("<a href=\"prev/page\">[^<]*Newer Posts</a>");
        assertThat(generated).containsMatch("<a href=\"next/page\">[^<]*Older Posts[^<]*</a>");
        assertThat(generated).doesNotContain("<h2");
        assertThat(generated).containsMatch(">\\s+Multiple Posts Content replacement\\s+<");
    }

    @Test
    public void testBlogPageTemplate() throws Exception {
        Templater.BlogPageData data = new Templater.BlogPageData(
                /* isIndex= */ false,
                "Site Title replacement",
                "http://Site URL/replacement/",
                "../../",
                "blog/cat/",
                "banner_image.jpg",
                "CSS replacement",
                "GA UID replacement",
                "Page Title replacement",
                "top_index.html",
                "prev/page",
                "next/page",
                "<div>SourceBlog Header as HTML</div>",
                "Post Title replacement",
                "2001-02-03",
                "A Category",
                "category/link",
                "page_file.html",
                "SourceContent replacement",
                "Rig4j Gen info",
                "main_image.jpg",
                "head description");
        String generated = mTemplater.generate(data);

        // --- This part is common with an article page
        assertThat(generated).containsMatch("property=\"og:url\"\\s+content=\"http://Site URL/replacement/blog/cat/page_file.html\"");
        assertThat(generated).containsMatch("property=\"og:title\"\\s+content=\"Post Title replacement\"");
        assertThat(generated).containsMatch("property=\"og:description\"\\s+content=\"head description\"");
        assertThat(generated).containsMatch("property=\"og:image\"\\s+content=\"http://Site URL/replacement/blog/cat/main_image.jpg\"");

        assertThat(generated).containsMatch("name=\"twitter:title\"\\s+content=\"Post Title replacement\"");
        // We don't generate Twitter meta-data for these yet
        assertThat(generated).containsMatch("name=\"twitter:description\"\\s+content=\"head description\"");
        assertThat(generated).containsMatch("name=\"twitter:image\"\\s+content=\"http://Site URL/replacement/blog/cat/main_image.jpg\"");

        assertThat(generated).contains("<title>Page Title replacement</title>");
        assertThat(generated).contains("background-image: url(\"banner_image.jpg\");");
        assertThat(generated).containsMatch("<style type=\"text/css\">[^<]+CSS replacement\\s*</style>");
        assertThat(generated).contains("<a href=\"http://Site URL/replacement/\">Site Title replacement</a>");
        assertThat(generated).contains("gtag('config', 'GA UID replacement');");

        // --- This part is specific to a blog page
        assertThat(generated).contains("<div>SourceBlog Header as HTML</div>");
        assertThat(generated).containsMatch("<span class=\"post-cat-text\">A Category</span>");
        assertThat(generated).containsMatch("<a href=\"prev/page\">[^<]*Newer Post</a>");
        assertThat(generated).containsMatch("<a href=\"next/page\">[^<]*Older Post[^<]*</a>");
        assertThat(generated).containsMatch("<h2[^>]*>2001-02-03 - Post Title replacement</h2>");
        assertThat(generated).containsMatch(">\\s+Content replacement\\s+<");
    }

    @Test
    public void testBlogPostTemplate() throws Exception {
        Templater.ArticleData data = new Templater.BlogPostData(
                "http://Site URL/replacement/",
                "../../",
                "blog/cat/",
                "Post Title replacement",
                "2001-02-03",
                "A Category",
                "category/link",
                "full/link",
                "extra/link",
                "Post SourceContent data"
        );
        String generated = mTemplater.generate(data);

        assertThat(generated).containsMatch("<h2[^>]+>2001-02-03 - Post Title replacement</h2>");
        assertThat(generated).containsMatch("<a[^>]+href=\"full/link\">");
        assertThat(generated).containsMatch("<a[^>]+href=\"extra/link\">Click here[^<]+</a>");
        assertThat(generated).containsMatch("<a[^>]+href=\"../../category/link\"[^>]+>A Category</a>");
        assertThat(generated).containsMatch(">\\s+Post Content data\\s+<");

    }

    @Test
    public void testHashContent() {
        String original = "<div class=\"bottom-container\">\n" +
                "    &nbsp;\n" +
                "    First contentrg-excl(to be excluded)rg-exclfrom the output.\n" +
                "    <!-- rg-excl( -->\n" +
                "    Second SourceContent to be excluded\n" +
                "    <!-- )rg-excl -->\n" +
                "    &nbsp;\n" +
                "</div>\n";

        String filtered = "<div class=\"bottom-container\">\n" +
                "    &nbsp;\n" +
                "    First contentfrom the output.\n" +
                "    <!--  -->\n" +
                "    &nbsp;\n" +
                "</div>\n";

        assertThat(mTemplater.hashContent(original)).isEqualTo(mTemplater.hashContent(filtered));
    }

    public static class TestTemplateData extends Templater.BaseData {

        private TestTemplateData(String siteTitle, String pageTitle) {
            super(siteTitle,
                    "absSiteLink/",
                    "relSiteLink/",
                    "fwdPageLink",
                    "css",
                    "GAUid",
                    pageTitle,
                    "pageFilename",
                    "relBannerLink");
        }

        @Override
        public String getTemplate(Flags flags, FileOps fileOps) throws IOException {
            fail("TestTemplateData.getTemplate is not defined in tests");
            return null;
        }
    }
}
