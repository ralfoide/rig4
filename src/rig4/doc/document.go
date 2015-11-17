package doc

type IDocument interface {
    Kind() string
    Content() string
}

// Document implements IDocument
type Document struct {
    kind    string
    content string
}

func NewDocument(kind, content string) *Document {
    d := &Document{ kind, content }
    return d
}

func (d *Document) Kind() string {
    return d.kind
}

func (d *Document) Content() string {
    return d.content
}
