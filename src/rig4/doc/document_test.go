package doc

import (
    "testing"
    "github.com/stretchr/testify/assert"
    "fmt"
)

func TestDocument(t *testing.T) {
    assert := assert.New(t)

    var d IDocument
    d = NewDocument("kind", "the content")

    assert.Equal("kind", d.Kind())
    assert.Equal("the content", d.Content())

    // d is an interface, which underlying type is a Document pointer
    assert.NotNil(d.(*Document))
    assert.Equal("&doc.Document{kind:\"kind\", content:\"the content\"}", fmt.Sprintf("%#v", d))
}
