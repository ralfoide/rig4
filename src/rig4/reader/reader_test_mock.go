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

func (m *MockReader) ReadDocuments(uri string) (<-chan doc.IDocument, error) {
    // Creates a unbuffered (blocking) channel
    c := make(chan doc.IDocument, 0)

    // A goroutine asynchronously creates and add the document to the channel.
    // The channel is closed once the last document has been sent.
    go func() {
        content := uri + "/" + strconv.Itoa(m.data)
        d := doc.NewDocument(m.kind, content)
        c <- d
        close(c)
    }()

    var e error
    if m.data == 42 {
        e = errors.New("Error " + m.kind + " " + strconv.Itoa(m.data))
    }

    return c, e
}
