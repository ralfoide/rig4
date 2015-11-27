package reader

import (
    "encoding/json"
    "flag"
    "fmt"
    "golang.org/x/net/context"
    "golang.org/x/oauth2"
    "golang.org/x/oauth2/google"
    "google.golang.org/api/drive/v2"
    "io/ioutil"
    "log"
    "net/http"
    "os"
    "path/filepath"
    "rig4/doc"
    "utils"
)

var GDOC_AUTH_INIT = flag.String("gdoc-auth-init", "", "Interactive initialization of OAuth web token.")
var GDOC_PATH_CLIENT_SECRET_JSON = flag.String("gdoc-path-client-secret-json", "~/.rig4/client_secret.json",
                                               "Path to load client_secret.json from Google Drive API.")
var GDOC_PATH_CREDENTIALS_TOKEN  = flag.String("gdoc-path-credentials-token", "~/.rig4/drive-api-token.json",
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
            if err == nil {
                log.Printf("[GDOC] Saved credential\n")
            } else {
                log.Printf("[GDOC] Error saving credential file: %v\n", err)
            }
        }
    }
    return config.Client(ctx, tok), err
}

// getTokenFromWeb uses Config to request a Token.
// It returns the retrieved Token.
func (g *GDocReader) getTokenFromWeb(config *oauth2.Config) (*oauth2.Token, error) {

    authURL := config.AuthCodeURL("state-token", oauth2.AccessTypeOffline)

    code := *GDOC_AUTH_INIT
    if code == "" {
        log.Printf(`
[GDOC] Please indicate how you want to generate and enter the auth code.
The program will print a URL which, once used, gives you an auth code
that you need to input back here.
Option 1: Use --gdoc-auth-init=interactive flag to read the auth code from stdin.
Option 2: Use --gdoc-auth-init=<code> using the code given on the web page.
You only need to do that once -- after that a token is stored in the file
indicated by --gdoc-path-credentials-token.

For option 2, go to the following link in your browser, then invoke this
again with --gdoc-auth-init=<code>:

%v

`, authURL)
        return nil, fmt.Errorf("[GDOC] Please provide a --gdoc-auth-init flag.")
    }

    if code == "interactive" {
        log.Printf(`
Go to the following link in your browser:

%v

Then type the authorization code:

`, authURL)

        if _, err := fmt.Scan(&code); err != nil {
            return nil, fmt.Errorf("[GDOC] Unable to read authorization code: %v", err)
        }
    }

    tok, err := config.Exchange(oauth2.NoContext, code)
    if err != nil {
      err = fmt.Errorf("[GDOC] Unable to retrieve token from web: %v", err)
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
      return nil, err
    }
    t := &oauth2.Token{}
    err = json.NewDecoder(f).Decode(t)
    defer f.Close()
    return t, err
}

// saveToken uses a file path to create a file and store the
// token in it.
func (g *GDocReader) saveToken(file string, token *oauth2.Token) error {
    log.Printf("[GDOC] Saving credential file to: %s\n", file)
    f, err := os.Create(file)
    if err != nil {
      return err
    }
    defer f.Close()
    err = json.NewEncoder(f).Encode(token)
    return err
}

// ----

func (g *GDocReader) findIzuFiles(d *drive.Service) *drive.File {
    var firstFile *drive.File
    q := "title contains '[izumi]' and fullText contains '[izu:'"
    log.Println("[GDOC] File query: ", q)
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
                log.Printf("[GDOC] %s (%s)\n", i.Title, i.Id)
                if firstFile == nil {
                    firstFile = i
                }
            }
        } else {
            log.Println("No files found.")
        }
        pageToken := reply.NextPageToken
        if pageToken == "" {
            log.Println("Last page")
            break
        }
    }
    return firstFile
}

func (g *GDocReader) getFileContent(h *http.Client, f *drive.File) {
    log.Println("File title       : ", f.Title)
    log.Println("File description : ", f.Description)
    log.Println("File download URL: ", f.DownloadUrl)
    log.Println("File Export Links: ", len(f.ExportLinks))
    for k, v := range f.ExportLinks {
        log.Printf("- [%v] : %v\n", k, v)
    }
    for _, k := range []string { "text/plain", "application/rtf", "text/html" } {
        url := f.ExportLinks[k]
        log.Printf("\n===== [%v] : %v\n\n", k, url)
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
    log.Printf("Content:\n%v\n", string(body))
}

