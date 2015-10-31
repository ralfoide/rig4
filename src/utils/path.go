package utils

import (
    "os/user"
    "path/filepath"
)

func ExpandUserPath(path string) string {
    if path[:2] == "~/" {
        if usr, err := user.Current(); err == nil {
            path = filepath.Join(usr.HomeDir, path[2:])
        }
    }
    return path
}
