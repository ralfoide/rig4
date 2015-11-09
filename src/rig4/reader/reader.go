package reader

import (
    "log"
)

type IReader interface {
    Init() error
    Name() string
    ReadAll(uri string) (string, error)
}

var readers = map[string]IReader{}

func AddReader(r IReader) {
    readers[r.Name()] = r
    if err := r.Init(); err != nil {
        log.Fatalf("[%s] %s", r.Name(), err)
    }
}

func GetReader(name string) IReader {
    if v, ok := readers[name]; ok {
        return v
    }
    return nil
}
