package doc

// IDocument extends IId
type IDocument interface {
    Kind() string
    Content() string
    Id() string
    Tags() map[string] string
}

// Document implements IDocument, IId
type Document struct {
    kind    string
    content string
    id      string
    tags    map[string] string
}

func NewDocument(kind, id, content string) *Document {
    d := &Document{kind: kind, id: id, content: content }
    d.tags = make(map[string] string, 0)
    return d
}

func (d *Document) Kind() string {
    return d.kind
}

func (d *Document) Content() string {
    return d.content
}

func (d *Document) Tags() map[string] string {
    return d.tags
}

// IId
func (d *Document) Id() string {
    return d.id
}

