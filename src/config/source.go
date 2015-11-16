package config

import (
    "strings"
    "errors"
    "regexp"
)

// Sources syntax:
// - "sources" config line contains either literal URI definitions or source references.
// - a source reference is another source config line that can also contain either URI
//   definitions or more source references.
type Sources []*Source

// A source represents an origin where to read Rig data from.
// A source has a kind (its type) and a URI. The kind represents the
// reader kind -- the actual object used to read from the source.
// The URI is an "opaque" data string passed to the reader as-is.
type Source struct{
    kind string
    uri string
}

// Source Line syntax BNF:
// SOURCE_LINE  := ( LITERAL | REF ) ( "," SOURCE_LINE )
// REF          := [a-z]+[a-zA-Z0-9_-]+
// LITERAL      := UNQUOTED | QUOTED
// UNQUOTED     := [a-z]+ ":" [^,]+
// QUOTED       := [a-z]+ ":" \" [^"]+ \"
//
// kind : uri with spaces but no commas [, another source]
// kind : "uri, with commas" [, another source]
// FindStringSubmatch substrings:
// 0 = whole source line.
// 1 = matched group before the first comma, either valid kind:uri or invalid (useful for errors.)
// 2 = "kind" part of the subgroup #1, not valid if empty.
// 3 = "uri" part of the subgroup #1, not valid if empty. Maybe be double-quoted.
// 4 = the rest of the line, with a possible leading comma.
var re_source = regexp.MustCompile("^\\s*(([a-z]+)\\s*:\\s*([^\"][^,]+|\"[^\"]+\")|[a-z]+|[^,]*)\\s*(,?.*)")

func NewSources() Sources {
    return make(Sources, 0)
}

func NewSource(kind, uri string) *Source {
    return &Source{kind, uri}
}

func ParseSources(sources string) (Sources, error) {
    var err error
    s := make(Sources, 0)

    sources = strings.Replace(sources, "\n", " ", -1)
    sources = strings.Replace(sources, "\r", " ", -1)

    for sources != "" {
        m := re_source.FindStringSubmatch(sources)

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

        sources = m[4]
        if sources == "" {
            break
        }

        if sources[0] == ',' {
            sources = sources[1:]
            continue
        }

        err = errors.New("[CONFIG] Extra source trailing content: '" + sources + "'. Did you forget a comma?")
        break;
    }

    return s, err
}
