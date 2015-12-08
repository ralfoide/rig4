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

    err := r.checkSources()
    assert.NotNil(err)
    assert.Contains(err.Error(), "No sources configured")

    r.sources = append(r.sources, config.NewSource("abc", "some uri 1"))
    r.sources = append(r.sources, config.NewSource("def", "some uri 2"))

    err = r.checkSources()
    assert.NotNil(err)
    assert.Contains(err.Error(), "No reader 'abc' exists for source 'some uri 1'")

    InitMockReaders(r)

    err = r.checkSources()
    assert.Nil(err)
}

func TestReadSources(t *testing.T) {
    assert := assert.New(t)

    r := NewRig4()

    r.sources = append(r.sources, config.NewSource("abc", "some uri 1"))
    r.sources = append(r.sources, config.NewSource("def", "some uri 2"))

    InitMockReaders(r)

    r.readers.GetReader("abc").(*reader.MockReader).Data = 12
    r.readers.GetReader("def").(*reader.MockReader).Data = 14

    err := r.readSources()
    assert.Nil(err)
    assert.Equal(2, len(r.docs.Range()))
    assert.Equal("abc", r.docs.Range()[0].Kind())
    assert.Equal("some uri 1/12", r.docs.Range()[0].Content())

    assert.Equal("def", r.docs.Range()[1].Kind())
    assert.Equal("some uri 2/14", r.docs.Range()[1].Content())
}

func TestReadSources_Duplicates(t *testing.T) {
    assert := assert.New(t)

    r := NewRig4()

    r.sources = append(r.sources, config.NewSource("abc", "some uri 1"))
    r.sources = append(r.sources, config.NewSource("abc", "some uri 1"))
    r.sources = append(r.sources, config.NewSource("def", "some uri 2"))
    r.sources = append(r.sources, config.NewSource("def", "some uri 2"))

    InitMockReaders(r)

    r.readers.GetReader("abc").(*reader.MockReader).Data = 12
    r.readers.GetReader("def").(*reader.MockReader).Data = 14

    err := r.readSources()
    assert.Nil(err)
    assert.Equal(2, len(r.docs.Range()))
    assert.Equal("abc", r.docs.Range()[0].Kind())
    assert.Equal("some uri 1/12", r.docs.Range()[0].Content())

    assert.Equal("def", r.docs.Range()[1].Kind())
    assert.Equal("some uri 2/14", r.docs.Range()[1].Content())
}
