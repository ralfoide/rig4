package config

import (
    "strings"
    "errors"
    "regexp"
)

type Source struct{
    kind string
    uri string
}

type Sources []*Source

// Line syntax:
// kind : uri with spaces but no commas [, another source]
// kind : "uri, with commas" [, another source]
// FindStringSubmatch substrings:
// 0 = whole source line.
// 1 = matched group before the first comma, either valid kind:uri or invalid (useful for errors.)
// 2 = "kind" part of the subgroup #1, not valid if empty.
// 3 = "uri" part of the subgroup #1, not valid if empty. Maybe be double-quoted.
// 4 = the rest of the line, with a possible leading comma.
var re_source = regexp.MustCompile("^\\s*(([a-z]+)\\s*:\\s*([^\"][^,]+|\"[^\"]+\")|[^,]*)\\s*(,?.*)")

func NewSources(config_line string) (Sources, error) {
    var err error
    s := make(Sources, 0)

    config_line = strings.Replace(config_line, "\n", " ", -1)
    config_line = strings.Replace(config_line, "\r", " ", -1)

    for config_line != "" {
        m := re_source.FindStringSubmatch(config_line)

        if m[1] != "" {
            kind := strings.TrimSpace(m[2])
            uri := strings.TrimSpace(m[3])

            if kind == "" || uri == "" {
                err = errors.New("[CONFIG] Invalid source syntax: '" + m[1] + "'. Expected: 'kind:uri [,...]'")
                break
            }

            n := len(uri)
            if uri[0] == '"' && uri[n - 1] == '"' {
                uri = uri[1:n - 1]
            }

            s = append(s, &Source{kind, uri})
        }

        config_line = m[4]
        if config_line == "" {
            break
        }

        if config_line[0] == ',' {
            config_line = config_line[1:]
            continue
        }

        err = errors.New("[CONFIG] Extra source trailing content: '" + config_line + "'. Did you forget a comma?")
        break;
    }

    return s, err
}
