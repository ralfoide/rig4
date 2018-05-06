package com.alflabs.rig4.blog;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.common.truth.Truth.assertThat;

public class CatFilterTest {
    public @Rule MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testNull() throws Exception {
        CatFilter filter = new CatFilter(null);

        assertThat(filter.isEmpty()).isTrue();
        assertThat(filter.matches("foo")).isFalse();
    }

    @Test
    public void testEmpty() throws Exception {
        CatFilter filter = new CatFilter("");

        assertThat(filter.isEmpty()).isTrue();
        assertThat(filter.matches("foo")).isFalse();
    }

    @Test
    public void testComment() throws Exception {
        CatFilter filter = new CatFilter("# everything after # is a comment");

        assertThat(filter.isEmpty()).isTrue();
    }

    @Test
    public void testMatchesAll() throws Exception {
        CatFilter filter = new CatFilter("  .*  ");

        assertThat(filter.isEmpty()).isFalse();
        assertThat(filter.matches("a")).isTrue();
        assertThat(filter.matches("foo")).isTrue();
        assertThat(filter.matches(" _ ")).isTrue();
        assertThat(filter.matches(" ! ")).isTrue();

        // An empty category is never matched
        assertThat(filter.matches(""   )).isFalse();
        assertThat(filter.matches("   ")).isFalse();
    }

    @Test
    public void testMatchesPatterns() throws Exception {
        CatFilter filter = new CatFilter(" a.c.* , two words, foo, name[0-9]+ # comment here ");

        assertThat(filter.isEmpty()).isFalse();

        assertThat(filter.matches("a")).isFalse();
        assertThat(filter.matches("name_0")).isFalse();

        // Name matching is not case sensitive, and is trimmed
        assertThat(filter.matches(" foo     ")).isTrue();
        assertThat(filter.matches(" Foo     ")).isTrue();

        assertThat(filter.matches("two words")).isTrue();
        assertThat(filter.matches("Two Words")).isTrue();

        assertThat(filter.matches("Name0   ")).isTrue();
        assertThat(filter.matches(" Name12 ")).isTrue();
        assertThat(filter.matches(" Name33 ")).isTrue();
        assertThat(filter.matches("  Name42")).isTrue();

        assertThat(filter.matches("abcWhatever")).isTrue();
        assertThat(filter.matches("a!cWhatever")).isTrue();
        assertThat(filter.matches("a_cWhatever")).isTrue();
        assertThat(filter.matches("abc_def_123")).isTrue();
    }
}
