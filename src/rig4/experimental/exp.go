package experimental
import (
    "flag"
    "golang.org/x/net/html"
    "io/ioutil"
    "log"
    "net/url"
    "os"
    "path/filepath"
    "regexp"
    "rig4/reader"
    "strings"
)


var EXP = flag.Bool("exp", false, "Enable experimental")
var EXP_GDOC_ID = flag.String("exp-doc-id", "", "Exp gdoc id")
var EXP_DEST_DIR= flag.String("exp-dest-dir", ".", "Exp dest dir")
var EXP_GA_UID= flag.String("exp-ga-uid", "", "Exp GA UID")
var EXP_DEBUG = flag.Bool("exp-debug", false, "Debug experimental")

func MainExp() {

    gd := reader.NewGDocReader()
    if err1 := gd.Init(); err1 != nil {
        log.Fatalln(err1)
    }

    doc_master, err2 := gd.ReadFileById(*EXP_GDOC_ID, "text/plain")
    if err2 != nil {
        log.Fatalln(err2)
    }

    line_re := regexp.MustCompile("^([a-z0-9]+.html)\\s+([a-zA-Z0-9_-]+)\\s*")

    dest_dir := *EXP_DEST_DIR

    for _, line := range strings.Split(doc_master.Content(), "\n") {
        line := strings.TrimSpace(line)
        fields := line_re.FindStringSubmatch(line)
        if fields == nil || len(fields) != 3 {
            continue
        }

        dest_name := fields[1]
        file_id := fields[2]

        log.Printf("Process document: %s\n", dest_name)
        log.Printf("         Reading: %s\n", file_id)

        doc_html, err3 := gd.ReadFileById(file_id, "text/html")
        if err3 != nil {
            log.Fatalln(err3)
        }
        str_html := doc_html.Content()
        title := doc_html.Tags()["gdoc-title"]

        if *EXP_DEBUG {
            tmp_name := filepath.Join("/tmp/", dest_name)
            log.Printf("         DEBUG  : %s [len: %d]\n", tmp_name, len(str_html))
            ioutil.WriteFile(filepath.Join(os.TempDir(), dest_name), []byte(str_html), 0644)
        }

        str_html = rewriteHtml(str_html, title)

        if dest_dir != "" {
            dest_name = filepath.Join(dest_dir, dest_name)
        }

        log.Printf("         Writing: %s [len: %d]\n", dest_name, len(str_html))
        if err4 := ioutil.WriteFile(dest_name, []byte(str_html), 0644); err4 != nil {
            log.Fatalln(err4)
        }
    }
}

var HREF_RE1 = regexp.MustCompile(
    `href="(https://www.google.com/url\?q=http[^&]+&amp;sa=D&amp;usg=[a-zA-Z0-9_-]+)"`)
var HREF_RE2 = regexp.MustCompile(
    `href="https://www.google.com/url\?q=(http[^&]+)&amp;sa=D&amp;usg=[a-zA-Z0-9_-]+"`)

var GA_SCRIPT = `<script>
  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');

  ga('create', 'GA-UID', 'auto');
  ga('send', 'pageview');

</script>`

func rewriteHtml(str_html, title string) string {
    str_html = strings.Replace(str_html, "</p>", "</p>\n", -1)
    str_html = HREF_RE1.ReplaceAllStringFunc(str_html, func(s string) string {
        m := HREF_RE1.FindStringSubmatch(s)
        if u, err := url.Parse(m[1]); err == nil {
            // Valid URL. Extract 'q' parameter.
            q := u.Query().Get("q")
            return `href="` + q + `"`
        } else {
            // Invalid URL. Just raw-transform.
            r := HREF_RE2.FindStringSubmatch(s)
            return `href="` + r[1] + `"`
        }
    })

    if title != "" {
        title_html := html.EscapeString(title)
        str_html = strings.Replace(str_html, "</head>", "<title>" + title_html + "</title></head>", -1)
    }

    ga := *EXP_GA_UID
    if ga != "" {
        ga = strings.Replace(GA_SCRIPT, "GA-UID", ga, -1)
        str_html = strings.Replace(str_html, "</body>", ga + "</body>", -1)
    }

    return str_html
}
