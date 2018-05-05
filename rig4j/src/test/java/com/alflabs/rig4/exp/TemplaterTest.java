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
        mTemplater = new Templater(mFlags, mTiming);
        mTemplater.declareFlags();
    }

    @Test
    public void testVarReplacement() throws Exception {
        String template = "<h2> {{.SiteTitle}} - {{.PageTitle}} </h2>";
        Templater templater = new Templater(mFlags, mTiming, template);
        String generated = templater.generate(new TestTemplateData("A Site Title", "A Page Title"));
        assertThat(generated).isEqualTo("<h2> A Site Title - A Page Title </h2>");
    }

    @Test
    public void testVarReplacement_NullIsEmpty() throws Exception {
        String template = "<h2> {{.SiteTitle}} - {{.PageTitle}} </h2>";
        Templater templater = new Templater(mFlags, mTiming, template);
        String generated = templater.generate(new TestTemplateData(null, ""));
        assertThat(generated).isEqualTo("<h2>  -  </h2>");
    }

    @Test
    public void testIfEmpty() throws Exception {
        String template = "<h2> {{.SiteTitle}} {{If.PageTitle}}- {{.PageTitle}} {{Endif}}</h2>";
        Templater templater = new Templater(mFlags, mTiming, template);

        String generated1 = templater.generate(new TestTemplateData("A Site", "A Page"));
        assertThat(generated1).isEqualTo("<h2> A Site - A Page </h2>");

        String generated2 = templater.generate(new TestTemplateData("A Site", ""));
        assertThat(generated2).isEqualTo("<h2> A Site </h2>");

        String generated3 = templater.generate(new TestTemplateData("A Site", null));
        assertThat(generated3).isEqualTo("<h2> A Site </h2>");
    }

    @Test
    public void testIfEqual() throws Exception {
        String template = "<h2> {{.SiteTitle}} {{If.PageTitle == .SiteTitle}}eq {{.PageTitle}} {{Endif}}</h2>";
        Templater templater = new Templater(mFlags, mTiming, template);

        String generated1 = templater.generate(new TestTemplateData("A Site", "A Page"));
        assertThat(generated1).isEqualTo("<h2> A Site </h2>");

        String generated2 = templater.generate(new TestTemplateData("A Site", "A Site"));
        assertThat(generated2).isEqualTo("<h2> A Site eq A Site </h2>");
    }

    @Test
    public void testIfNotEqual() throws Exception {
        String template = "<h2> {{.SiteTitle}} {{If.PageTitle != .SiteTitle}}+ {{.PageTitle}} {{Endif}}</h2>";
        Templater templater = new Templater(mFlags, mTiming, template);

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
                "<meta property=\"og:url\"         content=\"{{.SiteBaseUrl}}{{.PageFilename}}\" />\n" +
                "<meta property=\"og:type\"        content=\"article\" />\n" +
                "<meta property=\"og:title\"       content=\"{{.PageTitle}}\" />\n" +
                "<meta property=\"og:description\" content=\"{{.Description}}\" />\n" +
                "\n" +
                "<title>{{.PageTitle}}</title>\n" +
                "{{.Css}}\n" +
                "</head>\n" +
                "        <a href=\"{{.SiteBaseUrl}}\">{{.SiteTitle}}</a>\n" +
                "{{.Content}}\n" +
                "    ga('create', '{{.GAUid}}', 'auto');\n";

        Templater templater = new Templater(mFlags, mTiming, template);

        String generated = templater.generate(Templater.ArticleData.create(
                "CSS replacement",
                "GA UID replacement",
                "Page Title replacement",
                "Page Filename replacement",
                "Site Title replacement",
                "http://Site URL/replacement/",
                "Banner replacement",
                "Content replacement\n" +
                "Multiple content."));

        assertThat(generated).isEqualTo("" +
                "<!doctype html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "<meta property=\"og:url\"         content=\"http://Site URL/replacement/Page Filename replacement\" />\n" +
                "<meta property=\"og:type\"        content=\"article\" />\n" +
                "<meta property=\"og:title\"       content=\"Page Title replacement\" />\n" +
                "<meta property=\"og:description\" content=\"\" />\n" +
                "\n" +
                "<title>Page Title replacement</title>\n" +
                "CSS replacement\n" +
                "</head>\n" +
                "        <a href=\"http://Site URL/replacement/\">Site Title replacement</a>\n" +
                "Content replacement\n" +
                "Multiple content.\n" +
                "    ga('create', 'GA UID replacement', 'auto');\n");

    }

    @Test
    public void testComplexTemplate_IfEmpty() throws Exception {
        String template = "" +
                "{{If.NonExistent}}<!doctype html>\n" +
                "<html lang=\"en\">\n" +
                "<meta property=\"og:url\"         content=\"{{.SiteBaseUrl}}{{.PageFilename}}\" />\n" +
                "<meta property=\"og:type\"        content=\"article\" />\n" +
                "<head>{{EndIf}}\n" +
                "<meta property=\"og:title\"       content=\"{{.PageTitle}}\" />\n" +
                "{{IF.Description}}<meta property=\"og:description\" content=\"{{.Description}}\" />{{ENDIF}}\n" +
                "{{if.content}}{{.Content}}{{endif}}\n" +
                "{{if.GAUid}}ga('create', '{{.GAUid}} is null', 'auto');{{EndIf}}\n";

        Templater templater = new Templater(mFlags, mTiming, template);

        String generated = templater.generate(Templater.ArticleData.create(
                "CSS replacement",
                null,           // If.Var accepts both null and empty strings
                "Page Title replacement",
                "Page Filename replacement",
                "Site Title replacement",
                "http://Site URL/replacement/",
                "Banner replacement",
                "Content replacement\n" +
                        "Multiple content."));

        assertThat(generated).isEqualTo("" +
                "\n" +
                "<meta property=\"og:title\"       content=\"Page Title replacement\" />\n" +
                "\n" +
                "Content replacement\n" +
                "Multiple content.\n" +
                "\n");

    }

    @Test
    public void testArticleTemplate() throws Exception {
        Templater.ArticleData data = Templater.ArticleData.create(
                "CSS replacement",
                "GA UID replacement",
                "Page Title replacement",
                "Page Filename replacement",
                "Site Title replacement",
                "http://Site URL/replacement/",
                "Banner replacement",
                "Content replacement first line\n" +
                        "Content replacement second line.");
        String generated = mTemplater.generate(data);

        assertThat(generated).containsMatch("property=\"og:url\"\\s+content=\"http://Site URL/replacement/Page Filename replacement\"");
        assertThat(generated).containsMatch("property=\"og:title\"\\s+content=\"Page Title replacement\"");
        // We don't generate FB OG meta-data for these yet
        assertThat(generated).doesNotContain("property=\"og:description\"");
        assertThat(generated).doesNotContain("property=\"og:image\"");

        assertThat(generated).containsMatch("name=\"twitter:title\"\\s+content=\"Page Title replacement\"");
        // We don't generate Twitter meta-data for these yet
        assertThat(generated).doesNotContain("name=\"twitter:description\"");
        assertThat(generated).doesNotContain("name=\"twitter:image\"");

        assertThat(generated).contains("<title>Page Title replacement</title>");
        assertThat(generated).contains("background-image: url(\"Banner replacement\");");
        assertThat(generated).containsMatch("<style type=\"text/css\">[^<]+CSS replacement\\s*</style>");
        assertThat(generated).contains("<a href=\"http://Site URL/replacement/\">Site Title replacement</a>");
        assertThat(generated).contains("ga('create', 'GA UID replacement', 'auto');");
        assertThat(generated).containsMatch(">\\s+Content replacement first line\\s+Content replacement second line.\\s+<");
    }

    @Test
    public void testBlogPageTemplate() throws Exception {
        Templater.ArticleData data = Templater.BlogPageData.create(
                "CSS replacement",
                "GA UID replacement",
                "Page Title replacement",
                "Page Filename replacement",
                "Site Title replacement",
                "http://Site URL/replacement/",
                "Banner replacement",
                "Content replacement",
                "<div>Blog Header as HTML</div>",
                "2001-02-03",
                "Post Title replacement");
        String generated = mTemplater.generate(data);

        // --- This part is common with an article page
        assertThat(generated).containsMatch("property=\"og:url\"\\s+content=\"http://Site URL/replacement/Page Filename replacement\"");
        assertThat(generated).containsMatch("property=\"og:title\"\\s+content=\"Page Title replacement\"");
        // We don't generate FB OG meta-data for these yet
        assertThat(generated).doesNotContain("property=\"og:description\"");
        assertThat(generated).doesNotContain("property=\"og:image\"");

        assertThat(generated).containsMatch("name=\"twitter:title\"\\s+content=\"Page Title replacement\"");
        // We don't generate Twitter meta-data for these yet
        assertThat(generated).doesNotContain("name=\"twitter:description\"");
        assertThat(generated).doesNotContain("name=\"twitter:image\"");

        assertThat(generated).contains("<title>Page Title replacement</title>");
        assertThat(generated).contains("background-image: url(\"Banner replacement\");");
        assertThat(generated).containsMatch("<style type=\"text/css\">[^<]+CSS replacement\\s*</style>");
        assertThat(generated).contains("<a href=\"http://Site URL/replacement/\">Site Title replacement</a>");
        assertThat(generated).contains("ga('create', 'GA UID replacement', 'auto');");

        // --- This part is specific to a blog page
        assertThat(generated).contains("<div>Blog Header as HTML</div>");
        assertThat(generated).contains("<hr/><h2>2001-02-03 - Post Title replacement</h2>");
        assertThat(generated).containsMatch(">\\s+Content replacement\\s+<");
    }

    @Test
    public void testBlogPostTemplate() throws Exception {
        Templater.ArticleData data = Templater.BlogPostData.create(
                "http://Site URL/replacement/",
                "Post Content data",
                "2001-02-03",
                "Post Title replacement",
                "extra link/");
        String generated = mTemplater.generate(data);

        assertThat(generated).contains("<h2>2001-02-03 - Post Title replacement</h2>");
        assertThat(generated).contains("<a href=\"http://Site URL/replacement/extra link/\">Click here to read more...</a>");
        assertThat(generated).containsMatch(">\\s+Post Content data\\s+<");

    }

    public static class TestTemplateData extends Templater.BaseData {

        private TestTemplateData(String siteTitle, String pageTitle) {
            super(  "css",
                    "GAUid",
                    pageTitle,
                    "pageFilename",
                    siteTitle,
                    "siteBaseUrl",
                    "bannerFilename");
        }

        @Override
        public String getTemplate(Flags flags) throws IOException {
            fail("TestTemplateData.getTemplate is not defined in tests");
            return null;
        }
    }
}
