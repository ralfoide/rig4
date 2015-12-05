package doc

// IDocument extends IId
type IDocument interface {
    Kind() string
    Content() string
    Id() string
}

// Document implements IDocument, IId
type Document struct {
    kind    string
    content string
    id      string
}

func NewDocument(kind, id, content string) *Document {
    d := &Document{kind: kind, id: id, content: content }
    return d
}

func (d *Document) Kind() string {
    return d.kind
}

func (d *Document) Content() string {
    return d.content
}

// IId
func (d *Document) Id() string {
    return d.id
}
