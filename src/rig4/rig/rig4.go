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

func Main() {
    flag.Parse()
    CONFIG.ReadFile(utils.ExpandUserPath(*CONFIG_FILE))
    CONFIG.UpdateFlags(flag.CommandLine)

    initReaders()

    sources, err := getConfigSources()
    if err != nil {
        log.Fatalln(err)
    }

    // Check sources are properly configured and valid
    err = checkSources(sources)
    if err != nil {
        log.Fatalln(err)
    }

    // Read all documents from the given sources
    docs, err := readSources(sources)
    if err != nil {
        log.Fatalln(err)
    }
    log.Printf("Found %d documents\n", len(docs))
}

func initReaders() {
    log.Println("[READERS] Initialize")
    reader.AddReader(reader.NewGDocReader())
    reader.AddReader(reader.NewFileReader())
}

func getConfigSources() (config.Sources, error) {
    return config.ParseSources(*SOURCES, CONFIG)
}

func checkSources(sources config.Sources) error {
    log.Printf("[READERS] Checking %d sources\n", len(sources))
    if len(sources) == 0 {
        return fmt.Errorf("[READERS] No sources configured. Check your config file.")
    }
    for _, s := range sources {
        if r := reader.GetReader(s.Kind()); r == nil {
            return fmt.Errorf("[READERS] No reader '%s' exists for source '%s'\n", s.Kind(), s.URI())
        }
    }
    return nil
}

func readSources(sources config.Sources) ([]doc.IDocument, error) {
    docs := make([]doc.IDocument, 0)
    for _, s := range sources {
        var err error
        if docs, err = readSource(s, docs); err != nil {
            return docs, err
        }
    }
    return docs, nil
}

func readSource(s config.ISource, docs []doc.IDocument) ([]doc.IDocument, error) {
    log.Printf("[READERS] Read source %s:%s\n", s.Kind(), s.URI())
    r := reader.GetReader(s.Kind())
    dr, err := r.ReadDocuments(s.URI())
    for _, d := range dr {
        docs = append(docs, d)
    }
    return docs, err
}
