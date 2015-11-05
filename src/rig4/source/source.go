package source

import (
    "log"
)

type Source interface {
    Init() error
    ReadAll(uri string) (string, error)
}

var sources = map[string] Source {}

func InitSources() {
    sources["gdoc"] = &GDocSource{}
    if err := sources["gdoc"].Init(); err != nil {
        log.Fatalf("%s", err)
    }
}


