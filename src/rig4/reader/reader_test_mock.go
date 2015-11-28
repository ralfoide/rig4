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
    data int
}

func NewMockReader(name string) *MockReader {
    return &MockReader{name, 0}
}

func (m *MockReader) Kind() string {
    return m.kind;
}

func (m *MockReader) Init() error {
    m.data = 1
    return nil
}

func (m *MockReader) ReadDocuments(uri string) ([]doc.IDocument, error) {
    docs := make([]doc.IDocument, 0)

    content := uri + "/" + strconv.Itoa(m.data)
    d := doc.NewDocument(m.kind, content)
    docs = append(docs, d)

    var e error
    if m.data == 42 {
        e = errors.New("Error " + m.kind + " " + strconv.Itoa(m.data))
    }

    return docs, e
}
