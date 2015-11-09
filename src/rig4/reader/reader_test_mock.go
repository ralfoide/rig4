package reader

import (
    "strconv"
    "errors"
)

// -----

// Implements IReader
type MockReader struct {
    name string
    data int
}

func NewMockReader(name string) *MockReader {
    return &MockReader{name, 0}
}

func (m *MockReader) Name() string {
    return m.name;
}

func (m *MockReader) Init() error {
    m.data = 1
    return nil
}

func (m *MockReader) ReadAll(uri string) (string, error) {
    s := uri + "/" + strconv.Itoa(m.data)
    var e error
    if m.data == 42 {
        e = errors.New("Error " + m.name + " " + strconv.Itoa(m.data))
    }
    return s, e
}
