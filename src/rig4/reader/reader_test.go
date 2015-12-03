package reader

import (
    "testing"
    "github.com/stretchr/testify/assert"
    "errors"
)

// -----

func TestMockReader_Init(t *testing.T) {
    assert := assert.New(t)

    m := NewMockReader("mock")

    assert.Equal("mock", m.Kind())
    assert.Equal(0, m.Data)

    assert.Nil(m.Init())
    assert.Equal(1, m.Data)

    docs, err := m.ReadDocuments("blah:://foo")
    assert.Nil(err)
    assert.NotNil(docs)
    assert.Equal(1, len(docs))
    d := docs[0]
    assert.NotNil(d)
    assert.Equal("mock", d.Kind())
    assert.Equal("blah:://foo/1", d.Content())

    m.Data = 42
    docs, err = m.ReadDocuments("blah:://foo")
    assert.Equal(1, len(docs))
    d = docs[0]
    assert.NotNil(d)
    assert.Equal(errors.New("Error mock 42"), err)
    assert.Equal("mock", d.Kind())
    assert.Equal("blah:://foo/42", d.Content())
}

// -----

func TestAddReader_ClearReaders(t *testing.T) {
    assert := assert.New(t)

    rr := NewReaders()

    assert.Equal(0, len(*rr))
    assert.Nil(rr.GetReader("mock"))

    rr.AddReader(NewMockReader("mock"))
    assert.NotNil(rr.GetReader("mock"))
    assert.Equal(1, len(*rr))

    rr.ClearReaders()
    assert.Equal(0, len(*rr))
    assert.Nil(rr.GetReader("mock"))
}
