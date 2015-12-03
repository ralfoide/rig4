package rig

import (
    "config"
    "flag"
    "fmt"
    "log"
    "rig4/reader"
    "rig4/doc"
    "utils"
)

var CONFIG_FILE = flag.String("config", "~/.rig4rc", "Config file to read")
var SOURCES = flag.String("sources", "", "Sources of data")
var CONFIG = config.NewConfig()

// ----

type Rig4 struct {
    readers *reader.Readers
}

func NewRig4() *Rig4 {
    return &Rig4{readers: reader.NewReaders()}
}

// ----

func Main() {
    flag.Parse()
    CONFIG.ReadFile(utils.ExpandUserPath(*CONFIG_FILE))
    CONFIG.UpdateFlags(flag.CommandLine)

    r := NewRig4()

    r.initReaders()

    sources, err := r.getConfigSources()
    if err != nil {
        log.Fatalln(err)
    }

    // Check sources are properly configured and valid
    err = r.checkSources(sources)
    if err != nil {
        log.Fatalln(err)
    }

    // Read all documents from the given sources
    docs, err := r.readSources(sources)
    if err != nil {
        log.Fatalln(err)
    }
    log.Printf("Found %d documents\n", len(docs))
}

func (r *Rig4) initReaders() {
    log.Println("[READERS] Initialize")
    r.readers.AddReader(reader.NewGDocReader())
    r.readers.AddReader(reader.NewFileReader())
}

func (r *Rig4) getConfigSources() (config.Sources, error) {
    return config.ParseSources(*SOURCES, CONFIG)
}

func (r *Rig4) checkSources(sources config.Sources) error {
    log.Printf("[READERS] Checking %d sources\n", len(sources))
    if len(sources) == 0 {
        return fmt.Errorf("[READERS] No sources configured. Check your config file.")
    }
    for _, s := range sources {
        if r := r.readers.GetReader(s.Kind()); r == nil {
            return fmt.Errorf("[READERS] No reader '%s' exists for source '%s'\n", s.Kind(), s.URI())
        }
    }
    return nil
}

func (r *Rig4) readSources(sources config.Sources) ([]doc.IDocument, error) {
    docs := make([]doc.IDocument, 0)
    for _, s := range sources {
        var err error
        if docs, err = r.readSource(s, docs); err != nil {
            return docs, err
        }
    }
    return docs, nil
}

func (r *Rig4) readSource(s config.ISource, docs []doc.IDocument) ([]doc.IDocument, error) {
    log.Printf("[READERS] Read source %s:%s\n", s.Kind(), s.URI())
    reader_ := r.readers.GetReader(s.Kind())
    dr, err := reader_.ReadDocuments(s.URI())
    for _, d := range dr {
        docs = append(docs, d)
    }
    return docs, err
}
