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

    c, err := m.ReadDocuments("blah:://foo")
    assert.Nil(err)
    d := <-c
    assert.NotNil(d)
    assert.Equal("mock", d.Kind())
    assert.Equal("blah:://foo/1", d.Content())
    d, ok := <- c
    assert.Nil(d)
    assert.False(ok) // channel was closed and can't be read anymore

    m.data = 42
    c, err = m.ReadDocuments("blah:://foo")
    d = <-c
    assert.NotNil(d)
    assert.Equal(errors.New("Error mock 42"), err)
    assert.Equal("mock", d.Kind())
    assert.Equal("blah:://foo/42", d.Content())
    d, ok = <- c
    assert.Nil(d)
    assert.False(ok) // channel was closed and can't be read anymore
}

// -----

func TestAddReader(t *testing.T) {
    assert := assert.New(t)

    assert.Equal(0, len(readers))
    assert.Nil(GetReader("mock"))
    AddReader(NewMockReader("mock"))
    assert.NotNil(GetReader("mock"))
}
