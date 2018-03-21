package com.alflabs.rig4.flags;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/** Test for {@link Flag} */
public class FlagTest {

    @Test
    public void testFlagString_WithDescription() throws Exception {
        Flag.String_ flag = new Flag.String_("name", "value", "description");
        assertThat(flag.getName()).isEqualTo("name");
        assertThat(flag.getDescription()).isEqualTo("description");
        assertThat(flag.getValue()).isEqualTo("value");
        assertThat(flag.isDefaultValue()).isTrue();

        flag.setValue("");
        assertThat(flag.getValue()).isEqualTo("");
        assertThat(flag.isDefaultValue()).isFalse();
    }

    @Test
    public void testFlagString_NoDescription() throws Exception {
        Flag.String_ flag = new Flag.String_("name", "value", null);
        assertThat(flag.getDescription()).isNull();
    }

    @Test
    public void testFlagInt() throws Exception {
        Flag.Int flag = new Flag.Int("name", 42, "description");
        assertThat(flag.getName()).isEqualTo("name");
        assertThat(flag.getDescription()).isEqualTo("description");
        assertThat(flag.getValue()).isEqualTo(42);
        assertThat(flag.isDefaultValue()).isTrue();

        flag.setValue("0");
        assertThat(flag.getValue()).isEqualTo(0);
        assertThat(flag.isDefaultValue()).isFalse();
    }

    @Test
    public void testFlagBool() throws Exception {
        Flag.Bool flag = new Flag.Bool("name", false, "description");
        assertThat(flag.getName()).isEqualTo("name");
        assertThat(flag.getDescription()).isEqualTo("description");
        assertThat(flag.getValue()).isFalse();
        assertThat(flag.isDefaultValue()).isTrue();

        flag.setValue("true");
        assertThat(flag.getValue()).isTrue();
        assertThat(flag.isDefaultValue()).isFalse();
    }
}
