package doc

type IDocuments interface {
    Add(doc IDocument) IDocument
    Contains(doc_id string) bool
    Range() []IDocument
}

// Implements IDocuments
type Documents struct {
    docs []IDocument
}

func NewDocuments() IDocuments {
    d := &Documents{}
    return d
}

func (dd *Documents) Add(doc IDocument) IDocument {
    dd.docs = append(dd.docs, doc)
    return doc
}

func (dd *Documents) Contains(doc_id string) bool {
    for _, d := range dd.docs {
        if d.Id() == doc_id {
            return true
        }
    }
    return false
}

func (dd *Documents) Range() []IDocument {
    return dd.docs
}
