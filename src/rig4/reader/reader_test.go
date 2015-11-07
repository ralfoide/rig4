package source

import (
    "testing"
    "github.com/stretchr/testify/assert"
    "strconv"
    "errors"
)

// -----

type MockReader struct {
    data int
}

func (m *MockReader) Init() error {
    m.data = 1
    return nil
}

func (m *MockReader) ReadAll(uri string) (string, error) {
    s := uri + "/" + strconv.Itoa(m.data)
    var e error
    if m.data == 42 {
        e = errors.New("Error " + strconv.Itoa(m.data))
    }
    return s, e
}

// -----

func TestMockReader_Init(t *testing.T) {
    assert := assert.New(t)

    m := &MockReader{}

    assert.Equal(0, m.data)

    assert.Nil(m.Init())
    assert.Equal(1, m.data)

    s, err := m.ReadAll("blah:://foo")
    assert.Nil(err)
    assert.Equal("blah:://foo/1", s)

    m.data = 42
    s, err = m.ReadAll("blah:://foo")
    assert.Equal(errors.New("Error 42"), err)
    assert.Equal("blah:://foo/42", s)
}
