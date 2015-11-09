package rig4

import (
    "testing"
    "github.com/stretchr/testify/assert"
    "strconv"
    "errors"
    "rig4/reader"
)

// -----

func InitMockReaders(t *testing.T) {
    reader.AddReader(&reader.NewMockReader("gdoc"))
    reader.AddReader(&reader.NewMockReader("file"))
}
