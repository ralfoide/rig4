package rig

import (
    "testing"
    "github.com/stretchr/testify/assert"
    "rig4/reader"
    "config"
)

// -----

func InitMockReaders(r *Rig4) {
    r.readers.AddReader(reader.NewMockReader("abc"))
    r.readers.AddReader(reader.NewMockReader("def"))
}

func TestInitMockReaders(t *testing.T) {
    assert := assert.New(t)

    r := NewRig4()
    InitMockReaders(r)

    assert.NotNil(r.readers.GetReader("abc"))
    assert.NotNil(r.readers.GetReader("def"))
    assert.Nil(r.readers.GetReader("mock"))
    assert.Nil(r.readers.GetReader("whatever"))
}

func TestCheckSources(t *testing.T) {
    assert := assert.New(t)

    r := NewRig4()
    s := config.NewSources()

    err := r.checkSources(s)
    assert.NotNil(err)
    assert.Contains(err.Error(), "No sources configured")

    s = append(s, config.NewSource("abc", "some uri 1"))
    s = append(s, config.NewSource("def", "some uri 2"))

    err = r.checkSources(s)
    assert.NotNil(err)
    assert.Contains(err.Error(), "No reader 'abc' exists for source 'some uri 1'")

    InitMockReaders(r)

    err = r.checkSources(s)
    assert.Nil(err)
}

func TestReadSources(t *testing.T) {
    assert := assert.New(t)

    r := NewRig4()

    s := config.NewSources()
    s = append(s, config.NewSource("abc", "some uri 1"))
    s = append(s, config.NewSource("def", "some uri 2"))

    InitMockReaders(r)

    r.readers.GetReader("abc").(*reader.MockReader).Data = 12
    r.readers.GetReader("def").(*reader.MockReader).Data = 14

    docs, err := r.readSources(s)
    assert.Nil(err)
    assert.Equal(2, len(docs))
    assert.Equal("abc", docs[0].Kind())
    assert.Equal("some uri 1/12", docs[0].Content())

    assert.Equal("def", docs[1].Kind())
    assert.Equal("some uri 2/14", docs[1].Content())
}
