package rig4

import (
    "config"
    "flag"
    "os/user"
    "path/filepath"
)

var CONFIG_FILE string
var CONFIG = config.NewConfig()

func init() {
    filename := "~/.rig4rc"
    if usr, err := user.Current(); err == nil {
        filename = filepath.Join(usr.HomeDir, filename[2:])
    }
    flag.StringVar(&CONFIG_FILE, "config", filename, "Config file to read")
}

// ----

func Main() {
    flag.Parse()
    CONFIG.ReadFile(CONFIG_FILE)
    CONFIG.UpdateFlags(flag.CommandLine)
}

