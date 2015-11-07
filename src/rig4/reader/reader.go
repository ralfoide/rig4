package source

import (
    "log"
)

type Reader interface {
    Init() error
    ReadAll(uri string) (string, error)
}

var sources = map[string] Reader {}

func InitReaders() {
    sources["gdoc"] = &GDocReader{}
    if err := sources["gdoc"].Init(); err != nil {
        log.Fatalf("%s", err)
    }
}


