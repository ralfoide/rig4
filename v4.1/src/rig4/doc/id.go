package doc

// IId represents an object which has a "stable" id.
// The id can be any kind of string.
// An id is stable if subsequent invocations generate the *same* id
// for the "same" object. What represents the "same" object may be
// fuzzy. E.g. a post's stable id might be hash of its content or
// its creation date, and then may be overridden later with an izu tag.
type IId interface {
    Id() string
}
