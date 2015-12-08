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
func (g *FileReader) ReadDocuments(docs doc.IDocuments, uri string) error {
    matches, err := filepath.Glob(uri)
    if err == nil {
        sort.Strings(matches)
        for _, match := range matches {
            id := g.getId(match)
            if docs.Contains(id) {
                log.Printf("[FILE] Ignoring duplicated '%s'\n", id)
                continue
            }
            content, err2 := ioutil.ReadFile(match)
            if err2 != nil {
                log.Printf("[FILE] Failed to read '%s': %s\n", match, err2)
                err = err2
                break
            } else {
                log.Printf("[FILE] Reading '%s'\n", match)
                docs.Add(doc.NewDocument(g.Kind(), id, string(content)))
            }
        }
    }

    return err
}

func (g *FileReader) getId(path string) string {
    return g.Kind() + ":" + path
}

// -----

