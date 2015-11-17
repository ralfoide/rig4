package config

import (
    "testing"
    "github.com/stretchr/testify/assert"
    "errors"
)

func TestSource(t *testing.T) {
    assert := assert.New(t)

    var s ISource
    s = &Source{ "kind" , "some uri" }

    assert.Equal("kind", s.Kind())
    assert.Equal("some uri", s.URI())
}

func TestParseSources_Empty(t *testing.T) {
    assert := assert.New(t)

    // Disable debug printfs
    sourceDebug = 0

    // Empty string is ignored
    s, err := ParseSources("", nil)

    assert.Equal(0, len(s))
    assert.Nil(err)

    // Empty trimmed substrings are ignored. It may either be a code smell or convenient to allow that.
    s, err = ParseSources("   ,   ,   ", nil)

    assert.Equal(0, len(s))
    assert.Nil(err)
}

func TestParseSources_InvalidSyntax(t *testing.T) {
    assert := assert.New(t)

    s, err := ParseSources("abc some data", nil)

    assert.Equal(0, len(s))
    assert.Equal(errors.New("[CONFIG] 'sources' syntax error: unexpected ID, expecting ','"), err)
}

func TestParseSources_MissingCommaAfterQuote(t *testing.T) {
    assert := assert.New(t)

    s, err := ParseSources("abc : \"quoted stuff, with comma\" def : whatever ", nil)

    assert.Equal(1, len(s))
    assert.Equal(errors.New("[CONFIG] 'sources' syntax error: unexpected ID, expecting ','"), err)
    assert.Equal(Source{"abc", "quoted stuff, with comma"}, *s[0])
}

func TestParseSourcesUnquoted(t *testing.T) {
    assert := assert.New(t)

    s, err := ParseSources("abc : some source  ", nil)

    assert.Equal(1, len(s))
    assert.Nil(err)
    assert.Equal(Source{"abc", "some source"}, *s[0])
}

func TestParseSourcesQuoted(t *testing.T) {
    assert := assert.New(t)

    // Spaces between the quotes are not trimmed out
    s, err := ParseSources("abc : \" some quoted source \" ", nil)

    assert.Equal(1, len(s))
    assert.Nil(err)
    assert.Equal(Source{"abc", " some quoted source "}, *s[0])
}

func TestParseSources2(t *testing.T) {
    assert := assert.New(t)

    s, err := ParseSources("abc : some source,d:efg", nil)

    assert.Equal(2, len(s))
    assert.Nil(err)
    assert.Equal(Source{"abc", "some source"}, *s[0])
    assert.Equal(Source{"d",   "efg"},         *s[1])
}

func TestParseSources3(t *testing.T) {
    assert := assert.New(t)

    s, err := ParseSources("abc : some source , def : \"quoted stuff, with comma\" , ghi : !@#$%^&*()_+ ", nil)

    assert.Equal(3, len(s))
    assert.Nil(err)
    assert.Equal(Source{"abc", "some source"},              *s[0])
    assert.Equal(Source{"def", "quoted stuff, with comma"}, *s[1])
    assert.Equal(Source{"ghi", "!@#$%^&*()_+"},             *s[2])
}

func TestParseSources4(t *testing.T) {
    assert := assert.New(t)

    // All the spaces around separators are optional.
    s, err := ParseSources("abc:some source,def:\"quoted stuff, with comma\",ghi:!@#$%^&*()_+", nil)

    assert.Equal(3, len(s))
    assert.Nil(err)
    assert.Equal(Source{"abc", "some source"},              *s[0])
    assert.Equal(Source{"def", "quoted stuff, with comma"}, *s[1])
    assert.Equal(Source{"ghi", "!@#$%^&*()_+"},             *s[2])
}

func TestParseSources5(t *testing.T) {
    assert := assert.New(t)

    s, err := ParseSources(`
        file: /var/data/izu/*/*.izu,
        gdoc: title contains '[izumi]' and fullText contains '[izu:' `, nil)

    assert.Equal(2, len(s))
    assert.Nil(err)
    assert.Equal(Source{"file", "/var/data/izu/*/*.izu"}, *s[0])
    assert.Equal(Source{"gdoc", "title contains '[izumi]' and fullText contains '[izu:'"}, *s[1])
}

func TestParseSourcesReference(t *testing.T) {
    assert := assert.New(t)

    config := NewConfig()
    (*config)["ref"] = "def : ghi"

    s, err := ParseSources("abc : some source, ref", config)

    assert.Equal(2, len(s))
    assert.Nil(err)
    assert.Equal(Source{"abc", "some source"}, *s[0])
    assert.Equal(Source{"def", "ghi"},         *s[1])
}

func TestParseSourcesCircularReference(t *testing.T) {
    assert := assert.New(t)

    config := NewConfig()
    (*config)["ref"] = "def : ghi, ref"

    s, err := ParseSources("abc : some source, ref, ref", config)

    assert.Equal(2, len(s))
    assert.Nil(err)
    assert.Equal(Source{"abc", "some source"}, *s[0])
    assert.Equal(Source{"def", "ghi"},         *s[1])
}

func TestParseSourcesReferenceError(t *testing.T) {
    assert := assert.New(t)

    config := NewConfig()
    (*config)["ref"] = "def ghi"

    s, err := ParseSources("abc : some source, ref", config)

    assert.Equal(1, len(s))
    assert.Equal(errors.New("[CONFIG] 'ref' syntax error: unexpected ID, expecting ','"), err)
    assert.Equal(Source{"abc", "some source"}, *s[0])
}
