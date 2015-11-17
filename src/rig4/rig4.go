package rig4

import (
    "config"
    "flag"
    "utils"

    "rig4/reader"
    "log"
)

var CONFIG_FILE = flag.String("config", "~/.rig4rc", "Config file to read")
var SOURCES = flag.String("sources", "", "Sources of data")
var CONFIG = config.NewConfig()

func init() {
}

// ----

func Main() {
    flag.Parse()
    CONFIG.ReadFile(utils.ExpandUserPath(*CONFIG_FILE))
    CONFIG.UpdateFlags(flag.CommandLine)

    InitReaders()

    _ = GetConfigSources()
}

func InitReaders() {
    reader.AddReader(reader.NewGDocReader())
}

func GetConfigSources() config.Sources {
    s, err := config.ParseSources(*SOURCES, CONFIG)
    if err != nil {
        log.Fatal(err)
    }
    return s
}
