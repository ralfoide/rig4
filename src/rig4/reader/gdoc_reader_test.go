package reader

import (
    "fmt"
    "github.com/stretchr/testify/assert"
    "github.com/stretchr/testify/mock"
    "google.golang.org/api/drive/v2"
    "io/ioutil"
    "net/http"
    "reflect"
    "regexp"
    "strings"
    "testing"
)

// -----


type MockRoundTripper struct {
    mock.Mock
    t *testing.T
}

// Mock round trip that records the URL as a String
// and records a (status code, body, error) for the response.
func (t *MockRoundTripper) RoundTrip(req *http.Request) (*http.Response, error) {
    assert := assert.New(t.t)

    url := req.URL.String()
    assert.NotEmpty(url)

    args := t.Called(url)

    resp := &http.Response{}
    resp.StatusCode = args.Int(0)
    resp.Body = ioutil.NopCloser(strings.NewReader(args.String(1)))

    return resp, args.Error(2)
}

// -----

type MockGDocReader struct {
    GDocReader
    trip *MockRoundTripper
    mock mock.Mock
    t *testing.T
}

var re_map = regexp.MustCompile("[^\"]*\"([^\"]+)\":\"([^\"]+)\"(.*)")

func (g *MockGDocReader) MockQueryDo(query *drive.FilesListCall) (*drive.FileList, error) {
    assert := assert.New(g.t)

    // Use reflection to get the FilesListCall.opt_ map.
    // Note that reflect.Value.FieldByName() won't give us the value because
    // it's not an exported field. The work around that's good enough here
    // is to use the printf representation.
    v := reflect.ValueOf(query).Elem()
    opt_ := v.FieldByName("opt_")
    assert.NotNil(opt_)
    rep := fmt.Sprintf("%#v", opt_)

    // Rep is expected to be 'map[string]interface {}{"q":"some query"}'
    // Parse the map string representation and reconstruct the data map.
    head := "map[string]interface {}{"
    tail := "}"
    assert.True(strings.HasPrefix(rep, head))
    assert.True(strings.HasSuffix(rep, tail))
    kv := rep[len(head) : len(rep) - len(tail)]

    data := make(map[string]string)
    for {
        m := re_map.FindStringSubmatch(kv)
        if len(m) == 4 {
            k := m[1]
            v := m[2]
            kv = m[3]
            data[k] = v
        } else {
            break
        }
    }

    args := g.mock.Called(data)
    return args.Get(0).(*drive.FileList), args.Error(1)
}

func NewMockGDocReader(t *testing.T) *MockGDocReader {
    assert := assert.New(t)
    var err error

    g := &MockGDocReader{}
    assert.NotNil(g)
    g.t = t
    g.queryDo = g.MockQueryDo
    g.trip = &MockRoundTripper{t: t}
    g.client = &http.Client{Transport: g.trip}
    g.drive, err = drive.New(g.client)
    assert.Nil(err)

    return g
}

// ----

func TestGDocReader_findIzuFiles_Error(t *testing.T) {
    assert := assert.New(t)

    g := NewMockGDocReader(t)

    m := map[string]string { "q": "some query" }
    g.mock.On("MockQueryDo", m).Return(&drive.FileList{}, fmt.Errorf("some error"))

    files, err := g.findIzuFiles("some query")
    assert.NotNil(files)
    assert.Empty(files)
    assert.Equal(fmt.Errorf("some error"), err)
}

func TestGDocReader_findIzuFiles_1doc(t *testing.T) {
    assert := assert.New(t)

    g := NewMockGDocReader(t)

    list := &drive.FileList{}
    list.Items = append(list.Items, &drive.File{Title: "doc title", Id: "12345"})

    m := map[string]string { "q": "some query" }
    g.mock.On("MockQueryDo", m).Return(list, nil)

    files, err := g.findIzuFiles("some query")
    assert.Nil(err)
    assert.NotNil(files)
    assert.Equal(1, len(files))
    assert.Equal("doc title", files[0].Title)
    assert.Equal("12345", files[0].Id)
}

func TestGDocReader_findIzuFiles_2pages(t *testing.T) {
    assert := assert.New(t)

    g := NewMockGDocReader(t)

    list1 := &drive.FileList{}
    list1.Items = append(list1.Items, &drive.File{Title: "doc title 1", Id: "1"})
    list1.NextPageToken = "page2"
    m1 := map[string]string { "q": "some query" }
    g.mock.On("MockQueryDo", m1).Return(list1, nil)

    list2 := &drive.FileList{}
    list2.Items = append(list2.Items, &drive.File{Title: "doc title 2", Id: "2"})
    list2.NextPageToken = ""
    m2 := map[string]string { "q": "some query", "pageToken": "page2" }
    g.mock.On("MockQueryDo", m2).Return(list2, nil)

    files, err := g.findIzuFiles("some query")
    assert.Nil(err)
    assert.NotNil(files)
    assert.Equal(2, len(files))
    assert.Equal("doc title 1", files[0].Title)
    assert.Equal("1", files[0].Id)
    assert.Equal("doc title 2", files[1].Title)
    assert.Equal("2", files[1].Id)
}

func TestGDocReader_getFileAsDocument(t *testing.T) {
    assert := assert.New(t)

    g := NewMockGDocReader(t)

    f := &drive.File{}
    f.ExportLinks = map[string]string { "text/plain": "http://www.example.com" }
    f.Title = "doc title"

    g.trip.On("RoundTrip", "http://www.example.com").Return(200, "body response", nil)

    doc, err := g.getFileAsDocument(f)
    assert.Nil(err)
    assert.NotNil(doc)
    assert.Equal("gdoc", doc.Kind())
    assert.Equal("body response", doc.Content())
}

func TestGDocReader_getFileAsDocument_noTextPlain(t *testing.T) {
    assert := assert.New(t)

    g := NewMockGDocReader(t)

    f := &drive.File{}
    f.ExportLinks = map[string]string { "text/html": "http://www.example.com" }
    f.Title = "doc title"

    g.trip.On("RoundTrip", "http://www.example.com").Return(200, "body response",
        fmt.Errorf("some error"))

    doc, err := g.getFileAsDocument(f)
    assert.Nil(doc)
    assert.NotNil(err)
    assert.Contains(err.Error(), "No text/plain for file")
}

func TestGDocReader_getFileAsDocument_error(t *testing.T) {
    assert := assert.New(t)

    g := NewMockGDocReader(t)

    f := &drive.File{}
    f.ExportLinks = map[string]string { "text/plain": "http://www.example.com" }
    f.Title = "doc title"

    g.trip.On("RoundTrip", "http://www.example.com").Return(200, "body response",
        fmt.Errorf("some error"))

    doc, err := g.getFileAsDocument(f)
    assert.Nil(doc)
    assert.NotNil(err)
    assert.Contains(err.Error(), "Error downloading file")
}

func TestGDocReader_ReadDocuments(t *testing.T) {
    assert := assert.New(t)

    g := NewMockGDocReader(t)


    list1 := &drive.FileList{}
    list1.Items = append(list1.Items, &drive.File{Title: "doc title 1", Id: "1",
        ExportLinks: map[string]string { "text/plain": "http://www.example.com/1" }})
    list1.Items = append(list1.Items, &drive.File{Title: "doc title 2", Id: "2",
        ExportLinks: map[string]string { "text/plain": "http://www.example.com/2" }})
    list1.NextPageToken = "page2"
    m1 := map[string]string { "q": "some query" }
    g.mock.On("MockQueryDo", m1).Return(list1, nil)

    list2 := &drive.FileList{}
    list2.Items = append(list2.Items, &drive.File{Title: "doc title 3", Id: "3",
        ExportLinks: map[string]string { "text/plain": "http://www.example.com/3" }})
    list2.NextPageToken = ""
    m2 := map[string]string { "q": "some query", "pageToken": "page2" }
    g.mock.On("MockQueryDo", m2).Return(list2, nil)

    g.trip.On("RoundTrip", "http://www.example.com/1").Return(200, "body response 1", nil)
    g.trip.On("RoundTrip", "http://www.example.com/2").Return(200, "body response 2", nil)
    g.trip.On("RoundTrip", "http://www.example.com/3").Return(200, "body response 3", nil)

    docs, err := g.ReadDocuments("some query")
    assert.Nil(err)
    assert.NotNil(docs)
    assert.NotEmpty(docs)
    assert.Equal(3, len(docs))

    assert.Equal("gdoc", docs[0].Kind())
    assert.Equal("gdoc", docs[1].Kind())
    assert.Equal("gdoc", docs[2].Kind())

    assert.Equal("body response 1", docs[0].Content())
    assert.Equal("body response 2", docs[1].Content())
    assert.Equal("body response 3", docs[2].Content())
}

