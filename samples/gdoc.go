package main

import (
  "encoding/json"
  "fmt"
  "io/ioutil"
  "log"
  "net/http"
  "net/url"
  "os"
  "os/user"
  "path/filepath"

  "golang.org/x/net/context"
  "golang.org/x/oauth2"
  "golang.org/x/oauth2/google"
  "google.golang.org/api/drive/v2"
)

// getClient uses a Context and Config to retrieve a Token
// then generate a Client. It returns the generated Client.
func getClient(ctx context.Context, config *oauth2.Config) *http.Client {
    cacheFile, err := tokenCacheFile()
    if err != nil {
      log.Fatalf("Unable to get path to cached credential file. %v", err)
    }
    tok, err := tokenFromFile(cacheFile)
    if err != nil {
      tok = getTokenFromWeb(config)
      saveToken(cacheFile, tok)
    }
    return config.Client(ctx, tok)
}

// getTokenFromWeb uses Config to request a Token.
// It returns the retrieved Token.
func getTokenFromWeb(config *oauth2.Config) *oauth2.Token {
    authURL := config.AuthCodeURL("state-token", oauth2.AccessTypeOffline)
    fmt.Printf("Go to the following link in your browser then type the "+
      "authorization code: \n%v\n", authURL)

    var code string
    if _, err := fmt.Scan(&code); err != nil {
      log.Fatalf("Unable to read authorization code %v", err)
    }

    tok, err := config.Exchange(oauth2.NoContext, code)
    if err != nil {
      log.Fatalf("Unable to retrieve token from web %v", err)
    }
    return tok
}

// tokenCacheFile generates credential file path/filename.
// It returns the generated credential path/filename.
func tokenCacheFile() (string, error) {
    usr, err := user.Current()
    if err != nil {
      return "", err
    }
    tokenCacheDir := filepath.Join(usr.HomeDir, ".credentials")
    os.MkdirAll(tokenCacheDir, 0700)
    return filepath.Join(tokenCacheDir, url.QueryEscape("drive-api-quickstart.json")), err
}

// tokenFromFile retrieves a Token from a given file path.
// It returns the retrieved Token and any read error encountered.
func tokenFromFile(file string) (*oauth2.Token, error) {
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
func saveToken(file string, token *oauth2.Token) {
    fmt.Printf("Saving credential file to: %s\n", file)
    f, err := os.Create(file)
    if err != nil {
      log.Fatalf("Unable to cache oauth token: %v", err)
    }
    defer f.Close()
    json.NewEncoder(f).Encode(token)
}

func getDriveService() (*drive.Service, *http.Client) {
    ctx := context.Background()

    b, err := ioutil.ReadFile("client_secret.json")
    if err != nil {
      log.Fatalf("Unable to read client secret file: %v", err)
    }

    // RM 2015-09-20 we want both metadata *and* file content, read-only is good.
    // Consequently change the sample from DriveMetadataReadonlyScope to DriveReadonlyScope.
    // Tip: the drive auth is path of the OAuth2 config/token link so if this needs
    // to be changed then the .credential json file needs to be wiped out.
    config, err := google.ConfigFromJSON(b, drive.DriveReadonlyScope)
    if err != nil {
      log.Fatalf("Unable to parse client secret file to config: %v", err)
    }
    client := getClient(ctx, config)

    srv, err := drive.New(client)
    if err != nil {
      log.Fatalf("Unable to retrieve drive Client %v", err)
    }

    return srv, client
}

func findIzuFiles(d *drive.Service) *drive.File {
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
            log.Fatalf("Error in list query: %v", err)
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

func getFileContent(h *http.Client, f *drive.File) {
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
        downloadContent(h, url)
    }
}

func downloadContent(h *http.Client, url string) {
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

func main() {
    d, h := getDriveService()
    f := findIzuFiles(d)
    getFileContent(h, f)
}



