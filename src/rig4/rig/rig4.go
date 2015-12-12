package rig

import (
    "config"
    "flag"
    "fmt"
    "log"
    "rig4/reader"
    "rig4/doc"
    "utils"
    "rig4/experimental"
)

var CONFIG_FILE = flag.String("config", "~/.rig4rc", "Config file to read")
var SOURCES = flag.String("sources", "", "Sources of data")
var CONFIG = config.NewConfig()

// ----

type Rig4 struct {
    readers *reader.Readers
    sources config.Sources
    docs    doc.IDocuments
}

func NewRig4() *Rig4 {
    return &Rig4{
        readers: reader.NewReaders(),
        sources: config.NewSources(),
        docs: doc.NewDocuments(),
    }
}

// ----

func (r *Rig4) Main() {
    flag.Parse()
    CONFIG.ReadFile(utils.ExpandUserPath(*CONFIG_FILE))
    CONFIG.UpdateFlags(flag.CommandLine)

    if *experimental.EXP == true {
        experimental.MainExp()
        return
    }

    r.initReaders()

    var err error
    r.sources, err = r.getConfigSources()
    if err != nil {
        log.Fatalln(err)
    }

    // Check sources are properly configured and valid
    err = r.checkSources()
    if err != nil {
        log.Fatalln(err)
    }

    // Read all documents from the given sources
    err = r.readSources()
    if err != nil {
        log.Fatalln(err)
    }
    log.Printf("Found %d documents\n", len(r.docs.Range()))
}

func (r *Rig4) initReaders() {
    log.Println("[READERS] Initialize")
    r.readers.AddReader(reader.NewGDocReader())
    r.readers.AddReader(reader.NewFileReader())
}

func (r *Rig4) getConfigSources() (config.Sources, error) {
    return config.ParseSources(*SOURCES, CONFIG)
}

func (r *Rig4) checkSources() error {
    log.Printf("[READERS] Checking %d sources\n", len(r.sources))
    if len(r.sources) == 0 {
        return fmt.Errorf("[READERS] No sources configured. Check your config file.")
    }
    for _, s := range r.sources {
        if r := r.readers.GetReader(s.Kind()); r == nil {
            return fmt.Errorf("[READERS] No reader '%s' exists for source '%s'\n", s.Kind(), s.URI())
        }
    }
    return nil
}

func (r *Rig4) readSources() error {
    for _, s := range r.sources {
        if err := r.readSource(s); err != nil {
            return err
        }
    }
    return nil
}

func (r *Rig4) readSource(s config.ISource) error {
    log.Printf("[READERS] Read source %s:%s\n", s.Kind(), s.URI())
    reader_ := r.readers.GetReader(s.Kind())
    err := reader_.ReadDocuments(r.docs, s.URI())
    return err
}
