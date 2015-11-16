package config

import (
    "bufio"
    "flag"
    "log"
    "io"
    "os"
    "regexp"
    "strings"
)

// IConfigGetter interface represents an object that allows to get config values.
type IConfigGetter interface {
    Get(key, defaultValue string) string
}

// Config implements IConfigGetter
type Config map[string] string

var CONFIG_LINE_RE *regexp.Regexp = regexp.MustCompile("^\\s*([a-zA-Z0-9_.-]+)\\s*=\\s*(.*?)\\s*$")

// Creates a new Config object
func NewConfig() *Config {
    return &Config{}
}

// Fills a Config with values from a Reader interface
func (c *Config) Read(r io.Reader) error {
    buf := bufio.NewReader(r)
    return c.parse(buf)
}

// Fills a Config with values from an actual file
func (c *Config) ReadFile(filename string) error {
    log.Printf("[CONFIG] filename: %s\n", filename)
    f, err := os.Open(filename)
    if err != nil {
        if os.IsNotExist(err) {
            log.Printf("[CONFIG] %s not found\n", filename)
            return nil
        } else {
            log.Panicf("[CONFIG] Error reading %v: %#v", filename, err)
        }
    }
    defer f.Close()
    return c.Read(f)
}

// Internal parsing of Config lines.
func (c *Config) parse(r *bufio.Reader) error {
    for {
        line, err := r.ReadString('\n')
        if err == io.EOF {
            break
        } else if err != nil {
            log.Panicf("[CONFIG] Error reading config file: %v", err)
        }
        line = strings.TrimSpace(line)
        if line != "" {
            fields := CONFIG_LINE_RE.FindStringSubmatch(line)
            (*c)[fields[1]] = fields[2]
        }
    }
    log.Printf("[CONFIG] Read %d key/values from config file\n", len(*c))
    return nil
}

// Gets a Config value by key
func (c *Config) Get(key, defaultValue string) string {
    value, ok := (*c)[key]
    if ok {
        return value
    } else {
        return defaultValue
    }
}

// Updates all flags from a CommandLine's FlagSet by overriding flags
// with values from keys found in this config. Typical usage is the main
// reads a config file and uses this method to override or define all
// command-line flags. This way code only needs to read flags instead of
// checking both flags and/or the config.
func (c *Config) UpdateFlags(fs *flag.FlagSet) {
    var actual = make(map[string] *flag.Flag)
    fs.Visit(func(f *flag.Flag) {
        actual[f.Name] = f
    })

    fs.VisitAll(func(f *flag.Flag) {
        if _, visited := actual[f.Name]; !visited {
            if value, ok := (*c)[f.Name]; ok {
                fs.Set(f.Name, value)
                //--log.Printf("[CONFIG] DEBUG: SET %s = %s\n", f.Name, value)
            }
        }
    })
}

