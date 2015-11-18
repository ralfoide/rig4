package reader

import (
    "testing"
    "github.com/stretchr/testify/assert"
    "os"
    "fmt"
    "strings"
    "sort"
    "strconv"
    "utils"
)

// -----

func TestFileReader_1(t *testing.T) {
    assert := assert.New(t)

    p := utils.MkTempFile("some content")
    defer os.Remove(p)

    fr := NewFileReader()
    assert.Nil(fr.Init())
    assert.Equal("file", fr.Kind())

    c, err := fr.ReadDocuments(p)
    assert.Nil(err)
    assert.NotNil(c)
    // expect one document
    d, ok := <-c
    assert.NotNil(d)
    assert.True(ok)
    assert.Equal("file", d.Kind())
    assert.Equal("some content", d.Content())
    // no more documents
    d, ok = <-c
    assert.Nil(d)
    assert.False(ok)
}

func TestFileReader_3(t *testing.T) {
    assert := assert.New(t)

    n := 3
    p := make([]string, 0)
    for i := 0; i < n; i++ {
        s := strconv.Itoa(i)
        p = append(p, utils.MkTempFileInfix(s, "content #" + s))
    }
    sort.Strings(p)
    defer func () {
        for _, f := range(p) {
            os.Remove(f)
        }
    }()

    glob := p[0]
    pos := strings.LastIndex(glob, "_")
    assert.True(pos > 0)
    glob = glob[0 : pos + 1] + "*"
    assert.Equal(len(glob) - 1, strings.Index(glob, "*"))

    fr := NewFileReader()
    assert.Nil(fr.Init())
    assert.Equal("file", fr.Kind())

    c, err := fr.ReadDocuments(glob)
    assert.Nil(err)
    assert.NotNil(c)
    for i := 0; i < n; i++ {
        // expect one document
        d, ok := <-c
        assert.NotNil(d)
        assert.True(ok)
        assert.Equal("file", d.Kind())
        assert.Equal(fmt.Sprintf("content #%d", i), d.Content())
    }
    // no more documents
    d, ok := <-c
    assert.Nil(d)
    assert.False(ok)
}
