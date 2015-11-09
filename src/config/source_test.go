package config

import (
    "testing"
    "github.com/stretchr/testify/assert"
    "errors"
)

func TestNewSources_Empty(t *testing.T) {
    assert := assert.New(t)

    // Empty string is ignored
    s, err := NewSources("")

    assert.Equal(0, len(s))
    assert.Nil(err)

    // Empty trimmed substrings are ignored. It may either be a code smell or convenient to allow that.
    s, err = NewSources("   ,   ,   ")

    assert.Equal(0, len(s))
    assert.Nil(err)
}

func TestNewSources_InvalidSyntax(t *testing.T) {
    assert := assert.New(t)

    s, err := NewSources("abc some data")

    assert.Equal(0, len(s))
    assert.Equal(errors.New("[CONFIG] Invalid source syntax: 'abc some data'. Expected: 'kind:uri [,...]'"), err)
}

func TestNewSources_MissingCommaAfterQuote(t *testing.T) {
    assert := assert.New(t)

    s, err := NewSources("abc : \"quoted stuff, with comma\" def : whatever ")

    assert.Equal(1, len(s))
    assert.Equal(errors.New("[CONFIG] Extra source trailing content: 'def : whatever '. Did you forget a comma?"), err)
    assert.Equal(Source{"abc", "quoted stuff, with comma"}, *s[0])
}

func TestNewSources1(t *testing.T) {
    assert := assert.New(t)

    s, err := NewSources("abc : some source , def : \"quoted stuff, with comma\" , ghi : !@#$%^&*()_+ ")

    assert.Equal(3, len(s))
    assert.Nil(err)
    assert.Equal(Source{"abc", "some source"},              *s[0])
    assert.Equal(Source{"def", "quoted stuff, with comma"}, *s[1])
    assert.Equal(Source{"ghi", "!@#$%^&*()_+"},             *s[2])
}

func TestNewSources2(t *testing.T) {
    assert := assert.New(t)

    // All the spaces around separators are optional.
    s, err := NewSources("abc:some source,def:\"quoted stuff, with comma\",ghi:!@#$%^&*()_+")

    assert.Equal(3, len(s))
    assert.Nil(err)
    assert.Equal(Source{"abc", "some source"},              *s[0])
    assert.Equal(Source{"def", "quoted stuff, with comma"}, *s[1])
    assert.Equal(Source{"ghi", "!@#$%^&*()_+"},             *s[2])
}

func TestNewSources3(t *testing.T) {
    assert := assert.New(t)

    s, err := NewSources(`
        file: /var/data/izu/*/*.izu,
        gdoc: title contains '[izumi]' and fullText contains '[izu:' `)

    assert.Equal(2, len(s))
    assert.Nil(err)
    assert.Equal(Source{"file", "/var/data/izu/*/*.izu"}, *s[0])
    assert.Equal(Source{"gdoc", "title contains '[izumi]' and fullText contains '[izu:'"}, *s[1])
}

