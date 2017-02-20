package experimental
import (
    "bytes"
    "flag"
    "golang.org/x/net/html"
    "golang.org/x/net/html/atom"
    "image"
    "image/png"
    "io/ioutil"
    "log"
    "net/url"
    "os"
    "path/filepath"
    "regexp"
    "rig4/doc"
    "rig4/reader"
    "strconv"
    "strings"
    "errors"
    "sort"
    "path"
    "text/template"
)


var EXP = flag.Bool("exp", false, "Enable experimental")
var EXP_GDOC_ID = flag.String("exp-doc-id", "", "Exp gdoc id")
var EXP_DEST_DIR= flag.String("exp-dest-dir", ".", "Exp dest dir")
var EXP_GA_UID= flag.String("exp-ga-uid", "", "Exp GA UID")
var EXP_REWRITE_MODE = flag.Int("exp-rewrite-mode", RewriteUrls + UseTemplate, "Rewrite mode")
var EXP_DEBUG = flag.Bool("exp-debug", false, "Debug experimental")
var EXP_SITE_TITLE = flag.String("exp-site-title", "Site Title", "Web site title")
var EXP_SITE_BANNER = flag.String("exp-site-banner", "header.jpg", "Web site banner filename")

var GA_SCRIPT = `
  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');

  ga('create', 'GA-UID', 'auto');
  ga('send', 'pageview');
`

// TODO experiment with replacing stylesheets with local ones
// <link rel="stylesheet" href="material.blue-light_blue.css">
// <link rel="stylesheet" href="stylesheet.css">

var EXP_HTML_TEMPLATE = `<html lang="en">
<head>
<meta content="text/html; charset=UTF-8" http-equiv="content-type"/>
<link rel="stylesheet" href="https://code.getmdl.io/1.1.3/material.blue-light_blue.min.css">
<style type="text/css">
{{.Css}}

.demo-ribbon {
  width: 100%;
  height: 40vh;
  background-color: #3F51B5;
  -webkit-flex-shrink: 0;
      -ms-flex-negative: 0;
          flex-shrink: 0;
  background-image: url("{{.BannerFilename}}");
  background-position: center;
  background-size: 100% auto;
  background-repeat: no-repeat;
}

.demo-main {
  margin-top: -10vh;
  -webkit-flex-shrink: 0;
      -ms-flex-negative: 0;
          flex-shrink: 0;
}

.demo-header .mdl-layout__header-row {
  padding-left: 40px;
}

.demo-container {
  max-width: 1600px;
  width: calc(100% - 16px);
  margin: 0 auto;
}

.demo-content {
  border-radius: 2px;
  padding: 40px 56px 80px; /* top side bottom */
  margin-bottom: 80px;
}

.demo-layout.is-small-screen .demo-content {
  padding: 40px 28px;
}

.demo-content h3 {
  margin-top: 48px;
}

.demo-footer {
  padding-left: 40px;
}

.demo-footer .mdl-mini-footer--link-list a {
  font-size: 13px;
}

.demo-footer2 {
    display: -webkit-flex;
    display: -ms-flexbox;
    display: flex;
    -webkit-flex-flow: row wrap;
    -ms-flex-flow: row wrap;
    flex-flow: row wrap;
    -webkit-justify-content: space-between;
    -ms-flex-pack: justify;
    justify-content: space-between;

    padding: 32px 16px;
    color: #9e9e9e;
    background-color: #424242;
}

.demo-footer2-center {
    align-self: center;
    margin: auto;
}

</style>
<title>{{.PageTitle}}</title>
</head>
<body>
<div class="demo-layout mdl-layout mdl-layout--fixed-header mdl-js-layout mdl-color--grey-100">
  <div class="demo-ribbon"></div>
  <main class="demo-main mdl-layout__content">
    <div class="demo-container mdl-grid">
      <div class="mdl-cell mdl-cell--2-col mdl-cell--hide-tablet mdl-cell--hide-phone"></div>
      <div class="demo-content mdl-color--white mdl-shadow--4dp content mdl-color-text--grey-800 mdl-cell mdl-cell--8-col">

{{.Content}}

      </div>
    </div>
    <footer class="demo-footer demo-footer2">
      <center class="demo-footer2-center">
        <a class="mdl-button mdl-button--colored mdl-js-button mdl-js-ripple-effect" href=".">
          Back to main page
        </a>
        <br/>
        <span class="mdl-typography--display-2">{{.SiteTitle}}</span>
      </center>
    </footer>
  </main>
</div>

<script>
  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');

  ga('create', '{{.GAUid}}', 'auto');
  ga('send', 'pageview');
</script>
</body>
</html>
`

type TemplateData struct {
    Css             string
    BannerFilename  string
    GAUid           string
    PageTitle       string
    SiteTitle       string
    Content         string
}

type RewriteMode int

const (
    RewriteUrls = 1 << iota
    RewriteCss  = 1 << iota
    RewriteCss0 = 1 << iota
    RewriteHtml = 1 << iota
    UseTemplate = 1 << iota
)

type IExp interface {
    ReadFileById(id string, mimetype string) doc.IDocument
}

type IFileWriter interface {
    WriteFile(filename string, data []byte, perm os.FileMode) error
}

// Implements IExp, IFileWriter
type Exp struct {
    FileWriter      IFileWriter
    Reader          reader.IGDocReader
    Mode            RewriteMode
    dest_dir        string
    current_name    string
}

type HtmlEntry struct {
    DocId string
    DestName string
}

func NewHtmlEntry(docId, destName string) *HtmlEntry {
    return &HtmlEntry{DocId: docId, DestName: destName}
}

//---

func MainExp() {
    gd := reader.NewGDocReader()
    if err1 := gd.Init(); err1 != nil {
        log.Fatalln(err1)
    }

    exp := &Exp{Reader: gd, Mode: RewriteMode(*EXP_REWRITE_MODE)}
    exp.FileWriter = exp

    doc_master := exp.ReadIndex(*EXP_GDOC_ID)
    entries := exp.GetIndexEntries(doc_master)
    dest_dir := *EXP_DEST_DIR
    exp.ProcessEntries(entries, dest_dir)
}

func (e *Exp) WriteFile(filename string, data []byte, perm os.FileMode) error {
    return ioutil.WriteFile(filename, data, perm)
}

func (e *Exp) ReadFileById(id string, mimetype string) doc.IDocument {
    document, err := e.Reader.ReadFileById(id, mimetype)
    if err != nil {
        log.Fatalln(err)
    }
    return document
}

func (e *Exp) ReadIndex(id string) doc.IDocument {
    return e.ReadFileById(id, "text/plain")
}

func (e *Exp) ReadHtml(id string) doc.IDocument {
    return e.ReadFileById(id, "text/html")
}

func (e *Exp) GetIndexEntries(doc_master doc.IDocument) []*HtmlEntry {
    line_re := regexp.MustCompile("^([a-z0-9_-]+.html)\\s+([a-zA-Z0-9_-]+)\\s*")
    results := make([]*HtmlEntry, 0)

    for _, line := range strings.Split(doc_master.Content(), "\n") {
        line := strings.TrimSpace(line)
        fields := line_re.FindStringSubmatch(line)
        if fields == nil || len(fields) != 3 {
            continue
        }

        dest_name := fields[1]
        doc_id := fields[2]

        results = append(results, NewHtmlEntry(doc_id, dest_name))
    }

    return results
}


func (e *Exp) ProcessEntries(entries []*HtmlEntry, dest_dir string) {
    log.Printf("Found %d master entries\n", len(entries))
    for _, entry := range entries {
        dest_name := entry.DestName
        doc_id := entry.DocId

        log.Printf("Process document: %s\n", dest_name)
        log.Printf("         Reading: %s\n", doc_id)

        e.current_name = dest_name
        e.dest_dir = dest_dir

        doc_html := e.ReadHtml(doc_id)
        str_html := doc_html.Content()
        title := doc_html.Tags()["gdoc-title"]

        if *EXP_DEBUG {
            tmp_name := filepath.Join(os.TempDir(), dest_name)
            log.Printf("         DEBUG  : %s [len: %d]\n", tmp_name, len(str_html))
            e.FileWriter.WriteFile(tmp_name, []byte(str_html), 0644)
        }

        ga := *EXP_GA_UID
        if ga != "" {
            ga = strings.Replace(GA_SCRIPT, "GA-UID", ga, -1)
        }
        var err1 error
        str_html, err1 = e.ProcessEntry(str_html, title, ga)
        if err1 != nil {
            log.Fatalln(err1)
        }

        if dest_dir != "" {
            dest_name = filepath.Join(dest_dir, dest_name)
        }

        log.Printf("         Writing: %s [len: %d]\n", dest_name, len(str_html))
        if err2 := e.FileWriter.WriteFile(dest_name, []byte(str_html), 0644); err2 != nil {
            log.Fatalln(err2)
        }
    }
}

func (e *Exp) ProcessEntry(str_html, title, ga_script string) (string, error) {
    root, err1 := html.Parse(strings.NewReader(str_html))
    if err1 != nil {
        return "", err1
    }

    html_node := findChildNode(root, "html")
    head_node := findChildNode(html_node, "head")
    style_node := findChildNode(head_node, "style")
    body_node := findChildNode(html_node, "body")
    body_class := getAttribute(body_node, "class")

    style_node = findTextNode(style_node)
    style_str, css1, err2 := e.SimplifyStyles(style_node.Data, body_class)
    if err2 != nil {
        return "", err2
    }
    if style_str != "" {
        style_node.Data = style_str
    }
    css := css1

    if (e.Mode & UseTemplate) == 0 {
        if title != "" {
            insertOrReplaceNode(head_node, atom.Title, title, true)
        }

        if ga_script != "" {
            insertOrReplaceNode(body_node, atom.Script, ga_script, false)
        }
    }

    traverseAllNodes(body_node, func (node *html.Node) bool {
        ret := e.RewriteAttributes(node, css)
        node = rewriteYoutubeEmbed(node)
        return ret
    })

    if (e.Mode & UseTemplate) != 0 {
        t := template.Must(template.New("article").Parse(EXP_HTML_TEMPLATE))
        return e.RenderTemplate(t, body_node, style_node, title, *EXP_GA_UID)
    } else {
        var b bytes.Buffer
        err3 := html.Render(&b, root)
        return b.String(), err3
    }
}

func (e *Exp) RenderTemplate(
        temp *template.Template,
        body_node, style_node *html.Node,
        title, ga_uid string) (string, error) {
    data := &TemplateData{}

    data.BannerFilename = *EXP_SITE_BANNER
    data.GAUid = ga_uid
    data.PageTitle = title
    data.SiteTitle = *EXP_SITE_TITLE
    data.Css = style_node.Data
    data.Content = ""

    body_child := body_node.FirstChild
    for body_child != nil {
        if body_child.Type == html.ElementNode {
            var b1 bytes.Buffer
            err1 := html.Render(&b1, body_child)
            if err1 != nil {
                return "", err1
            }
            data.Content += b1.String()
        }
        body_child = body_child.NextSibling
    }

    var b2 bytes.Buffer
    err2 := temp.Execute(&b2, data)
    return b2.String(), err2
}

func rewriteYoutubeEmbed(node *html.Node) *html.Node {
    if node.Type == html.ElementNode && node.DataAtom == atom.A {
        href := getAttribute(node, "href")
        if u, err := url.Parse(href); err == nil {
            if u.Host == "www.youtube.com" && u.Path == "/watch" {
                rig4embed := u.Query().Get("rig4embed")
                if rig4embed != "" {
                    videoId := u.Query().Get("v")
                    width := u.Query().Get("width")
                    height := u.Query().Get("height")
                    frameborder := u.Query().Get("frameborder")
                    src := "https://www.youtube.com/embed/" + videoId

                    if width == "" {
                        width = "560"
                    }
                    if height == "" {
                        height = "315"
                    }
                    if frameborder == "" {
                        frameborder = "0"
                    }

                    node.DataAtom = atom.Iframe
                    node.Data = node.DataAtom.String()
                    node.Attr = append(node.Attr, html.Attribute{Key: "width", Val: width})
                    node.Attr = append(node.Attr, html.Attribute{Key: "height", Val: height})
                    node.Attr = append(node.Attr, html.Attribute{Key: "src", Val: src})
                    node.Attr = append(node.Attr, html.Attribute{Key: "frameborder", Val: frameborder})
                    node.Attr = append(node.Attr, html.Attribute{Key: "allowfullscreen"})
                }
            }
        }
    }
    return node
}

func findChildNode(root *html.Node, tag string) *html.Node {
    if root != nil {
        child := root.FirstChild
        for child != nil {
            if child.Type == html.ElementNode && child.Data == tag {
                return child
            }
            child = child.NextSibling
        }
    }
    return nil
}

func findTextNode(root *html.Node) *html.Node {
    if root != nil {
        child := root.FirstChild
        for child != nil {
            if child.Type == html.TextNode {
                return child
            }
            child = child.NextSibling
        }
    }
    return nil
}

func getAttribute(node *html.Node, name string) string {
    if node != nil {
        for _, a := range node.Attr {
            if a.Namespace == "" && a.Key == name {
                return a.Val
            }
        }
    }
    return ""
}

func insertOrReplaceNode(parent *html.Node, tag atom.Atom, content string, can_replace bool) {
    var tag_node *html.Node
    if can_replace {
       tag_node = findChildNode(parent, tag.String())
    }
    if tag_node == nil {
        tag_node = &html.Node{
            Type:     html.ElementNode,
            DataAtom: tag,
            Data:     tag.String(),
        }
        parent.InsertBefore(tag_node, nil)
    }

    text_node := findTextNode(tag_node)
    if text_node == nil {
        text_node = &html.Node{
            Type:   html.TextNode,
            Data:   content,
        }
        tag_node.InsertBefore(text_node, nil)
    } else {
        text_node.Data = content
    }
}

// Function that processes one node traversed. Returns true if the node
// must be kept or false if the node needs to be removed from the tree.
// TODO: node removal is not implemented yet.
type traverseFunc func (*html.Node) bool

// Traverses the nodes, deep-first.
func traverseAllNodes(root *html.Node, process traverseFunc) {
    if root != nil {
        for root != nil {
            traverseAllNodes(root.FirstChild, process)
            if root.Type == html.ElementNode {
                process(root)
            }
            root = root.NextSibling
        }
    }
}

func (e *Exp) RewriteUrl(str string) string {
    if u, err := url.Parse(str); err == nil {
        if u.Host == "www.google.com" && u.Path == "/url" {
            q := u.Query().Get("q")
            if q != "" {
                return q
            }
        } else if u.Host == "docs.google.com" && u.Path == "/drawings/image" {
            id := u.Query().Get("id")
            w, _ := strconv.Atoi(u.Query().Get("w"))
            h, _ := strconv.Atoi(u.Query().Get("h"))

            log.Printf("         Drawing: %s [%v x %v]\n", id, w, h)
            str = e.ProcessDrawing(id, w, h)
        }
    }
    return str
}

func (e *Exp) RewriteAttributes(node *html.Node, css CssMap) bool {
    if node == nil || len(node.Attr) == 0 {
        return true
    }

    rewrite_urls := (e.Mode & RewriteUrls) != 0

    for index := range node.Attr {
        // Note: for _, a := range Array return copies, not references; since we want
        // to change the elements, explicitly access to the array items via reference.
        a := &node.Attr[index]

        if rewrite_urls && a.Namespace == "" && (a.Key == "href" || a.Key == "src") {
            a.Val = e.RewriteUrl(a.Val)
        }
    }
    return true
}

func (e *Exp) SimplifyStyles(styles, body_class string) (string, CssMap, error) {
    if styles == "" {
        return "", nil, nil
    }

    css, err := parseStyles(styles)
    if err != nil {
        return "", nil, err
    }

    result := ""
    var keys []string
    for key := range css {
        keys = append(keys, key)
    }
    sort.Strings(keys)
    for _, selector := range keys {
        c := css[selector]
        if selector == body_class {
            result += c.String(selector) + "\n"
        } else {
            // This can be used to Keep the old value as a comment in the source
            // result += "/* " + c.String(selector) + " */\n"
            if c.CleanupAttrs(e.Mode) {
                result += c.String(selector) + "\n"
            }
        }
    }

    return result, css, nil
}

// ----------------------
// Drawings

func (e *Exp) ProcessDrawing(id string, w, h int) string {
    extension := "png"
    dest_name := path.Base(e.current_name)
    dest_name = strings.Replace(dest_name, ".html", "", -1)
    dest_name = strings.Replace(dest_name, ".", "_", -1)
    dest_name = dest_name + "_drawing_" + id + "." + extension
    dest_path := dest_name
    if e.dest_dir != "" {
        dest_path = filepath.Join(e.dest_dir, dest_path)
    }

    log.Printf("     Downloading: %s\n", dest_name)

    url := "https://docs.google.com/drawings/d/" + id + "/export/" + extension
    img, err1 := e.Reader.Get(url)
    if err1 != nil {
        log.Fatalln(err1)
    }

    if w > 0 && h > 0 {
        img = resizeImage(img, w, h)
    }

    if err2 := e.FileWriter.WriteFile(dest_path, img, 0644); err2 != nil {
        log.Fatalln(err2)
    }

    return dest_name
}

func resizeImage(input []byte, w, h int) []byte {
    result := input

    img, err1 := png.Decode(bytes.NewBuffer(input))
    if err1 == nil {
        bounds := img.Bounds()

        x1 := bounds.Max.X
        y1 := bounds.Max.Y
        x2 := bounds.Min.X
        y2 := bounds.Min.Y

        for y := bounds.Min.Y; y < bounds.Max.Y; y++ {
            for x := bounds.Min.X; x < bounds.Max.X; x++ {
                _, _, _, a := img.At(x, y).RGBA()
                if a != 0 {
                    if x < x1 {
                        x1 = x
                    } else if x > x2 {
                        x2 = x
                    }
                    if y < y1 {
                        y1 = y
                    } else if y > y2 {
                        y2 = y
                    }
                }
            }
        }

        rect := image.Rect(x1, y1, x2 + 1, y2 + 1)
        switch img2 := img.(type) {
        case *image.Gray:
            img = img2.SubImage(rect)
        case *image.Gray16:
            img = img2.SubImage(rect)
        case *image.NRGBA:
            img = img2.SubImage(rect)
        case *image.NRGBA64:
            img = img2.SubImage(rect)
        case *image.RGBA:
            img = img2.SubImage(rect)
        case *image.RGBA64:
            img = img2.SubImage(rect)
        default:
            log.Fatalln("Unknown PNG decoded IMAGE TYPE %T\n", img)
        }

        var buf bytes.Buffer
        png.Encode(&buf, img)
        result = buf.Bytes()
    }

    return result
}



// ----------------------
// Limited CSS parser

type CssAttr map[string] string

type CssMap map[string] CssAttr

func parseStyles(styles string) (CssMap, error) {
    result := make(CssMap, 0)
    styles = strings.TrimSpace(styles)
    for len(styles) > 0 {
        if strings.HasPrefix(styles, "@import") {
            // skip till next ;
            pos := strings.Index(styles, ";")
            if pos == -1 {
                return result, errors.New("Missing ; after @import in <style>")
            }
            styles = styles[pos + 1 : ]
        } else {
            start := strings.Index(styles, "{")
            end   := strings.Index(styles, "}")
            if start < 1 || end <= start {
                return result, errors.New("Invalid {...} in <style>")
            }
            selector := strings.TrimSpace(styles[0 : start])

            // Filter out selectors we don't care about. This is not a generic parser.
            // We only want class selectors (which start with a dot) and we don't want
            // id selectors or complex ones with rules.
            // if strings.HasPrefix(selector, ".") && !strings.ContainsAny(selector, "# ,>") {
            //
            // If the latest version, just grab everything.

            result[selector] = parseCssAttr(styles[start + 1 : end])


            styles = styles[end + 1 : ]
        }
        styles = strings.TrimSpace(styles)
    }
    return result, nil
}

func parseCssAttr(attributes string) CssAttr {
    attrs := make(CssAttr, 0)

    for len(attributes) > 0 {
        start := strings.Index(attributes, ":")
        end   := strings.Index(attributes, ";")
        if end == -1 {
            end = len(attributes)
        }
        if start > 0 && end > start {
            key := strings.TrimSpace(attributes[0 : start])
            val := strings.TrimSpace(attributes[start + 1 : end])
            if key != "" && val != "" {
                attrs[key] = val
            }
        }
        if end >= len(attributes) {
            break
        }
        attributes = attributes[end + 1 : ]
    }

    return attrs
}

func (c *CssAttr) String(selector string) string {
    s := selector + " { "

    var keys []string
    for key := range *c {
        keys = append(keys, key)
    }
    sort.Strings(keys)

    sep := false
    for _, key := range keys {
        if sep {
            s += "; "
        }
        s += key + ": " + (*c)[key]
        sep = true
    }
    s += " }"
    return s
}

// Returns true to keep the element, false to have it removed from the style list
func (c *CssAttr) CleanupAttrs(mode RewriteMode) bool {

    if (mode & RewriteCss0) != 0 {
        for key, val := range *c {
            switch {
            case key == "font-family" && !strings.Contains(val, "Consolas"):
                delete(*c, key)
            }
        }

    } else if (mode & RewriteCss) != 0 {
        for key, val := range *c {
            switch {
            case
                key == "font-family" && strings.Contains(val, "Consolas"),
                key == "font-style" && val != "normal" && val != "inherit",
                key == "font-weight" && val != "normal" && val != "inherit",
                key == "text-align" && val != "left" && val != "inherit",
                key == "color" && val != "#000000" && val != "inherit",
                key == "background-color" && val != "#ffffff" && val != "inherit":
                continue
            }
            delete(*c, key)
        }
    }

    return len(*c) > 0
}
