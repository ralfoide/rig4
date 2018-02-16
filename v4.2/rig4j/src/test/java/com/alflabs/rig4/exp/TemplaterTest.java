package com.alflabs.rig4.exp;

import com.alflabs.rig4.flags.Flags;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.common.truth.Truth.assertThat;


public class TemplaterTest {
    public @Rule MockitoRule mMockitoRule = MockitoJUnit.rule();

    private @Mock Flags mFlags;

    @Test
    public void testSimpleReplacements() throws Exception {
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

        Templater templater = new Templater(mFlags, template);

        String generated = templater.generate(Templater.TemplateData.create(
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

}
