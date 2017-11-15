package utils

// This file contains no tests.
// It contains utilities which are useful in tests.

import (
    "bytes"
    "fmt"
    "github.com/sergi/go-diff/diffmatchpatch"
    "io/ioutil"
    "log"
)

// Creates a temp file with the given content.
// Panics if the file cannot be created.
// Returns the file path, with a name pattern TEMPDIR/rig4test_<random>.
// Caller must delete the file e.g.
//   defer os.Remove(filepath)
func MkTempFile(content string) string {
    return MkTempFileInfix("", content)
}

// Creates a temp file with the given content.
// Panics if the file cannot be created.
// Returns the file path, with a name pattern TEMPDIR/rig4test_<infix><random>.
// Caller must delete the file e.g.
//   defer os.Remove(filepath)
func MkTempFileInfix(infix, content string) string {
    f, err := ioutil.TempFile("" /*dir*/, "rig4test_" + infix /*prefix*/)
    if err != nil {
        log.Panicf("mkTempFile failed: %#v\n", err)
    }
    defer f.Close()
    f.WriteString(content)
    return f.Name()
}

// ----

func StringDiff(string1, string2 string) string {
    dmp := diffmatchpatch.New()
    diffs := dmp.DiffMain(string1, string2, true /*checklines*/)

    var w bytes.Buffer
    for i, diff := range diffs {
        w.WriteString(fmt.Sprintf("%v. ", i))
        switch diff.Type {
        case diffmatchpatch.DiffInsert:
            w.WriteString("++")
        case diffmatchpatch.DiffDelete:
            w.WriteString("--")
        case diffmatchpatch.DiffEqual:
            w.WriteString("==")
        default:
            w.WriteString("Unknown")
        }
        w.WriteString(fmt.Sprintf(": '%v'\n", diff.Text))
    }
    return w.String()
}
