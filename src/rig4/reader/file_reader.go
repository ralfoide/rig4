package reader

import (
    "path/filepath"

    "rig4/doc"
    "io/ioutil"
    "log"
    "sort"
)

// -----

// Implements IReader
type FileReader struct {
}

func NewFileReader() *FileReader {
    return &FileReader{}
}

// Kind is "file"
func (g *FileReader) Kind() string {
    return "file";
}

// Never fails to initialize
func (g *FileReader) Init() (err error) {
    return nil
}

// URI is a path or a glob pattern compatible with Go's filepath.Glob()
// This returns one document per file matched using the glob pattern.
// The glob results are sorted using sort.Strings() for a consistent result.
func (g *FileReader) ReadDocuments(uri string) (<-chan doc.IDocument, error) {
    // Creates an unbuffered (blocking) channel
    c := make(chan doc.IDocument, 0)

    matches, err := filepath.Glob(uri)
    if err != nil {
        close(c)
    } else {
        sort.Strings(matches)
        go func() {
            defer close(c)
            for _, match := range matches {
                content, err := ioutil.ReadFile(match)
                if err != nil {
                    log.Printf("[FILE] Failed to read '%s': %s\n", match, err)
                } else {
                    log.Printf("[FILE] Reading '%s'\n", match)
                    d := doc.NewDocument(g.Kind(), string(content))
                    c <- d
                }
            }
        }()
    }

    return c, err
}

// -----

