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

type Readers map[string]IReader

func NewReaders() *Readers {
    return &Readers{}
}

func (rr *Readers) AddReader(r IReader) {
    (*rr)[r.Kind()] = r
    if err := r.Init(); err != nil {
        log.Fatalf("[%s] %s", r.Kind(), err)
    }
}

func (rr *Readers) GetReader(name string) IReader {
    if v, ok := (*rr)[name]; ok {
        return v
    }
    return nil
}

func (rr *Readers) ClearReaders() {
    for k, _ := range (*rr) {
        delete((*rr), k)
    }
}
