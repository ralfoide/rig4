package rig

import (
    "testing"
    "github.com/stretchr/testify/assert"
    "rig4/reader"
    "config"
)

// -----

func InitMockReaders() {
    reader.AddReader(reader.NewMockReader("abc"))
    reader.AddReader(reader.NewMockReader("def"))
}

func TestInitMockReaders(t *testing.T) {
    assert := assert.New(t)

    InitMockReaders()
    defer reader.ClearReaders()

    assert.NotNil(reader.GetReader("abc"))
    assert.NotNil(reader.GetReader("def"))
    assert.Nil(reader.GetReader("mock"))
    assert.Nil(reader.GetReader("whatever"))
}

func TestCheckSources(t *testing.T) {
    assert := assert.New(t)

    s := config.NewSources()

    err := checkSources(s)
    assert.NotNil(err)
    assert.Contains(err.Error(), "No sources configured")

    s = append(s, config.NewSource("abc", "some uri 1"))
    s = append(s, config.NewSource("def", "some uri 2"))

    err = checkSources(s)
    assert.NotNil(err)
    assert.Contains(err.Error(), "No reader 'abc' exists for source 'some uri 1'")

    InitMockReaders()
    defer reader.ClearReaders()

    err = checkSources(s)
    assert.Nil(err)
}

func TestReadSources(t *testing.T) {
    assert := assert.New(t)

    s := config.NewSources()
    s = append(s, config.NewSource("abc", "some uri 1"))
    s = append(s, config.NewSource("def", "some uri 2"))

    InitMockReaders()
    defer reader.ClearReaders()

    reader.GetReader("abc").(*reader.MockReader).Data = 12
    reader.GetReader("def").(*reader.MockReader).Data = 14

    docs, err := readSources(s)
    assert.Nil(err)
    assert.Equal(2, len(docs))
    assert.Equal("abc", docs[0].Kind())
    assert.Equal("some uri 1/12", docs[0].Content())

    assert.Equal("def", docs[1].Kind())
    assert.Equal("some uri 2/14", docs[1].Content())
}
