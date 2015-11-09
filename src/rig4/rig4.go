package rig4

import (
    "config"
    "flag"
    "utils"

    "rig4/reader"
)

var CONFIG_FILE = flag.String("config", "~/.rig4rc", "Config file to read")
var CONFIG = config.NewConfig()

func init() {
}

// ----

func Main() {
    flag.Parse()
    CONFIG.ReadFile(utils.ExpandUserPath(*CONFIG_FILE))
    CONFIG.UpdateFlags(flag.CommandLine)

    InitReaders()
}

func InitReaders() {
    reader.AddReader(&reader.NewGDocReader())
}
