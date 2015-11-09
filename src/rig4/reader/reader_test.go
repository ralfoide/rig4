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

    assert.Equal("mock", m.Name())
    assert.Equal(0, m.data)

    assert.Nil(m.Init())
    assert.Equal(1, m.data)

    s, err := m.ReadAll("blah:://foo")
    assert.Nil(err)
    assert.Equal("blah:://foo/1", s)

    m.data = 42
    s, err = m.ReadAll("blah:://foo")
    assert.Equal(errors.New("Error mock 42"), err)
    assert.Equal("blah:://foo/42", s)
}

// -----

func TestAddReader(t *testing.T) {
    assert := assert.New(t)

    assert.Equal(0, len(readers))
    assert.Nil(GetReader("mock"))
    AddReader(NewMockReader("mock"))
    assert.NotNil(GetReader("mock"))
}
