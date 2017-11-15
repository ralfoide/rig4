package reader

import (
    "strconv"
    "errors"
    "rig4/doc"
)

// -----

// Implements IReader
type MockReader struct {
    kind string
    Data int
}

func NewMockReader(name string) *MockReader {
    return &MockReader{kind: name, Data: 0}
}

func (m *MockReader) Kind() string {
    return m.kind;
}

func (m *MockReader) Init() error {
    m.Data = 1
    return nil
}

func (m *MockReader) ReadDocuments(docs doc.IDocuments, uri string) error {
    content := uri + "/" + strconv.Itoa(m.Data)
    id := uri
    if docs.Contains(id) {
        return nil
    }
    d := doc.NewDocument(m.kind, id, content)
    docs.Add(d)

    var e error
    if m.Data == 42 {
        e = errors.New("Error " + m.kind + " " + strconv.Itoa(m.Data))
    }

    return e
}
