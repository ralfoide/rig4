package rig4

import (
    "testing"
    "github.com/stretchr/testify/assert"
    "rig4/reader"
)

// -----

func InitMockReaders() {
    reader.AddReader(reader.NewMockReader("gdoc"))
    reader.AddReader(reader.NewMockReader("file"))
}

func TestInitMockReaders(t *testing.T) {
    assert := assert.New(t)

    InitMockReaders()

    assert.NotNil(reader.GetReader("gdoc"))
    assert.NotNil(reader.GetReader("file"))
    assert.Nil(reader.GetReader("mock"))
    assert.Nil(reader.GetReader("whatever"))
}
