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
    "rig4/doc"
)

// -----

func TestFileReader_1(t *testing.T) {
    assert := assert.New(t)

    p := utils.MkTempFile("some content")
    defer os.Remove(p)

    fr := NewFileReader()
    assert.Nil(fr.Init())
    assert.Equal("file", fr.Kind())

    docs := doc.NewDocuments()
    err := fr.ReadDocuments(docs, p)
    assert.Nil(err)
    assert.Equal(1, len(docs.Range()))
    d := docs.Range()[0]
    assert.NotNil(d)
    assert.Equal("file", d.Kind())
    assert.Equal("some content", d.Content())
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

    docs := doc.NewDocuments()
    err := fr.ReadDocuments(docs, glob)
    assert.Nil(err)
    assert.Equal(n, len(docs.Range()))
    for i := 0; i < n; i++ {
        d := docs.Range()[i]
        assert.NotNil(d)
        assert.Equal("file", d.Kind())
        assert.Equal(fmt.Sprintf("content #%d", i), d.Content())
    }
}
