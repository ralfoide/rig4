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
    assert.Equal(0, m.data)

    assert.Nil(m.Init())
    assert.Equal(1, m.data)

    docs, err := m.ReadDocuments("blah:://foo")
    assert.Nil(err)
    assert.NotNil(docs)
    assert.Equal(1, len(docs))
    d := docs[0]
    assert.NotNil(d)
    assert.Equal("mock", d.Kind())
    assert.Equal("blah:://foo/1", d.Content())

    m.data = 42
    docs, err = m.ReadDocuments("blah:://foo")
    assert.Equal(1, len(docs))
    d = docs[0]
    assert.NotNil(d)
    assert.Equal(errors.New("Error mock 42"), err)
    assert.Equal("mock", d.Kind())
    assert.Equal("blah:://foo/42", d.Content())
}

// -----

func TestAddReader(t *testing.T) {
    assert := assert.New(t)

    assert.Equal(0, len(readers))
    assert.Nil(GetReader("mock"))
    AddReader(NewMockReader("mock"))
    assert.NotNil(GetReader("mock"))
}
