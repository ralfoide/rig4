package reader

import (
    "log"
    "rig4/doc"
)

type IReader interface {
    // Initialize the reader. Some readers that require authentication (e.g. gdoc)
    // may return an error and request to be called explicitely with a --init flag
    // for interactive setup or request more command-line arguments.
    Init() error

    // Returns the kind of that reader.
    Kind() string

    // Read and returns a slice of documents and/or a read error.
    ReadDocuments(uri string) ([]doc.IDocument, error)
}

var readers = map[string]IReader{}

func AddReader(r IReader) {
    readers[r.Kind()] = r
    if err := r.Init(); err != nil {
        log.Fatalf("[%s] %s", r.Kind(), err)
    }
}

func GetReader(name string) IReader {
    if v, ok := readers[name]; ok {
        return v
    }
    return nil
}
