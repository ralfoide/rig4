package reader

import (
    "encoding/json"
    "flag"
    "fmt"
    "io/ioutil"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "utils"

    "golang.org/x/net/context"
    "golang.org/x/oauth2"
    "golang.org/x/oauth2/google"
    "google.golang.org/api/drive/v2"

    "rig4/doc"
)

var GDOC_PATH_CLIENT_SECRET_JSON = flag.String("gdoc_path_client_secret_json", "~/.rig4/client_secret.json", 
                                               "Path to load client_secret.json from Google Drive API.")
var GDOC_PATH_CREDENTIALS_TOKEN  = flag.String("gdoc_path_credentials_token", "~/.rig4/drive-api-token.json", 
                                               "Path to save the Drive API credential token.")

// -----

// Implements IReader
type GDocReader struct {
    drive   *drive.Service
    client  *http.Client
}

func NewGDocReader() *GDocReader {
    return &GDocReader{}
}

func (g *GDocReader) Kind() string {
    return "gdoc";
}

func (g *GDocReader) Init() (err error) {
    g.drive, g.client, err = g.getDriveService()
    return err
}

func (g *GDocReader) ReadDocuments(uri string) (<-chan doc.IDocument, error) {
    return nil, nil
}

// -----

func (g *GDocReader) getDriveService() (*drive.Service, *http.Client, error) {
    ctx := context.Background()
    
    b, err := ioutil.ReadFile(utils.ExpandUserPath(*GDOC_PATH_CLIENT_SECRET_JSON))
    if err != nil {
        err = fmt.Errorf("[GDOC] Unable to read client secret file: %v", err)
        return nil, nil, err
    }

    // We want both metadata *and* file content, read-only is good.
    // Tip: the drive auth is in the path of the OAuth2 config/token link so if this needs
    // to be changed then the .credential json file needs to be wiped out.
    config, err := google.ConfigFromJSON(b, drive.DriveReadonlyScope)
    if err != nil {
        err = fmt.Errorf("[GDOC] Unable to parse client secret file to config: %v", err)
        return nil, nil, err
    }
    client, err := g.getClient(ctx, config)
    if err != nil {
        return nil, client, err
    }

    srv, err := drive.New(client)
    if err != nil {
        err = fmt.Errorf("[GDOC] Unable to retrieve drive Client %v", err)
        return srv, client, err
    }

    return srv, client, nil
}

// -----
// The following is extracted from the Google Drive API sample code.

// getClient uses a Context and Config to retrieve a Token
// then generate a Client. It returns the generated Client.
func (g *GDocReader) getClient(ctx context.Context, config *oauth2.Config) (*http.Client, error) {
    var err error
    cacheFile, err := g.tokenCacheFile()
    if err != nil {
        log.Printf("[GDOC] Unable to get path to cached credential file. %v", err)
        return nil, err
    }
    tok, err := g.tokenFromFile(cacheFile)
    if err != nil {
        tok, err = g.getTokenFromWeb(config)
        if err == nil {
            err = g.saveToken(cacheFile, tok)
        }
    }
    return config.Client(ctx, tok), err
}


// getTokenFromWeb uses Config to request a Token.
// It returns the retrieved Token.
func (g *GDocReader) getTokenFromWeb(config *oauth2.Config) (*oauth2.Token, error) {
    authURL := config.AuthCodeURL("state-token", oauth2.AccessTypeOffline)
    fmt.Printf("Go to the following link in your browser then type the "+
      "authorization code: \n%v\n", authURL)

    var code string
    if _, err := fmt.Scan(&code); err != nil {
      err = fmt.Errorf("[GDOC] Unable to read authorization code %v", err)
    }

    tok, err := config.Exchange(oauth2.NoContext, code)
    if err != nil {
      err = fmt.Errorf("[GDOC] Unable to retrieve token from web %v", err)
    }
    return tok, err
}

// tokenCacheFile generates credential file path/filename.
// It returns the generated credential path/filename.
func (g *GDocReader) tokenCacheFile() (string, error) {
    path := utils.ExpandUserPath(*GDOC_PATH_CREDENTIALS_TOKEN)
    dir  := filepath.Dir(path)
    err  := os.MkdirAll(dir, 0700)
    return path, err
}

// tokenFromFile retrieves a Token from a given file path.
// It returns the retrieved Token and any read error encountered.
func (g *GDocReader) tokenFromFile(file string) (*oauth2.Token, error) {
    f, err := os.Open(file)
    if err != nil {
      return nil, fmt.Errorf("[GDOC] %v", err)
    }
    t := &oauth2.Token{}
    err = json.NewDecoder(f).Decode(t)
    defer f.Close()
    return t, fmt.Errorf("[GDOC] %v", err)
}

// saveToken uses a file path to create a file and store the
// token in it.
func (g *GDocReader) saveToken(file string, token *oauth2.Token) error {
    fmt.Printf("Saving credential file to: %s\n", file)
    f, err := os.Create(file)
    if err != nil {
      log.Printf("Unable to cache oauth token: %v", err)
      return err
    }
    defer f.Close()
    return json.NewEncoder(f).Encode(token)
}

// ----

func (g *GDocReader) findIzuFiles(d *drive.Service) *drive.File {
    var firstFile *drive.File
    q := "title contains '[izumi]' and fullText contains '[izu:'"
    fmt.Println("File query: ", q)
    pageToken := ""
    for {
        query := d.Files.List().Q(q)
        if pageToken != "" {
            query = query.PageToken(pageToken)
        }
        reply, err := query.Do()
        if err != nil {
            log.Panicf("Error in list query: %v", err)
        }
        if len(reply.Items) > 0 {
          for _, i := range reply.Items {
            fmt.Printf("%s (%s)\n", i.Title, i.Id)
            if firstFile == nil {
                firstFile = i
            }
          }
        } else {
          fmt.Println("No files found.")
        }
        pageToken := reply.NextPageToken
        if pageToken == "" {
            fmt.Println("Last page")
            break
        }
    }
    return firstFile
}

func (g *GDocReader) getFileContent(h *http.Client, f *drive.File) {
    fmt.Println("File title       : ", f.Title)
    fmt.Println("File description : ", f.Description)
    fmt.Println("File download URL: ", f.DownloadUrl)
    fmt.Println("File Export Links: ", len(f.ExportLinks))
    for k, v := range f.ExportLinks {
        fmt.Printf("- [%v] : %v\n", k, v)
    }
    for _, k := range []string { "text/plain", "application/rtf", "text/html" } {
        url := f.ExportLinks[k]
        fmt.Printf("\n===== [%v] : %v\n\n", k, url)
        if url == "" {
            continue
        }
        g.downloadContent(h, url)
    }
}

func (g *GDocReader) downloadContent(h *http.Client, url string) {
    resp, err := h.Get(url)
    if err != nil {
        log.Printf("Request error: %v", err)
        return
    }
    defer resp.Body.Close()
    body, err := ioutil.ReadAll(resp.Body)
    if err != nil {
        log.Printf("Read body error: %v", err)
    }
    fmt.Printf("Content:\n%v\n", string(body))
}

