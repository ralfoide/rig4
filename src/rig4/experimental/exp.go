package experimental
import (
    "bytes"
    "flag"
    "golang.org/x/net/html"
    "golang.org/x/net/html/atom"
    "io/ioutil"
    "log"
    "net/url"
    "os"
    "path/filepath"
    "regexp"
    "rig4/doc"
    "rig4/reader"
    "strings"
    "errors"
    "sort"
)


var EXP = flag.Bool("exp", false, "Enable experimental")
var EXP_GDOC_ID = flag.String("exp-doc-id", "", "Exp gdoc id")
var EXP_DEST_DIR= flag.String("exp-dest-dir", ".", "Exp dest dir")
var EXP_GA_UID= flag.String("exp-ga-uid", "", "Exp GA UID")
var EXP_REWRITE_MODE = flag.Int("exp-rewrite-mode", RewriteUrls | RewriteCss0, "Rewrite mode")
var EXP_DEBUG = flag.Bool("exp-debug", false, "Debug experimental")


var GA_SCRIPT = `
  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');

  ga('create', 'GA-UID', 'auto');
  ga('send', 'pageview');
`

type RewriteMode int

const (
    RewriteUrls = 1 << iota
    RewriteCss  = 1 << iota
    RewriteCss0 = 1 << iota
    RewriteHtml = 1 << iota
)

type IExp interface {
    ReadFileById(id string, mimetype string) doc.IDocument
}

// Implements IExp
type Exp struct {
    Reader  reader.IGDocReader
    Mode    RewriteMode
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

    doc_master := exp.ReadIndex(*EXP_GDOC_ID)
    entries := exp.GetIndexEntries(doc_master)
    dest_dir := *EXP_DEST_DIR
    exp.ProcessEntries(entries, dest_dir)
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
    for _, entry := range entries {
        dest_name := entry.DestName
        doc_id := entry.DocId

        log.Printf("Process document: %s\n", dest_name)
        log.Printf("         Reading: %s\n", doc_id)

        doc_html := e.ReadHtml(doc_id)
        str_html := doc_html.Content()
        title := doc_html.Tags()["gdoc-title"]

        if *EXP_DEBUG {
            tmp_name := filepath.Join(os.TempDir(), dest_name)
            log.Printf("         DEBUG  : %s [len: %d]\n", tmp_name, len(str_html))
            ioutil.WriteFile(tmp_name, []byte(str_html), 0644)
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
        if err2 := ioutil.WriteFile(dest_name, []byte(str_html), 0644); err2 != nil {
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

    var css CssMap
//    if (e.Mode & RewriteCss) != 0 {
        style_node = findTextNode(style_node)
        style_str, css1, err2 := e.SimplifyStyles(style_node.Data, body_class)
        if err2 != nil {
            return "", err2
        }
        if style_str != "" {
            style_node.Data = style_str
        }
        css = css1
//    }

    if title != "" {
        insertOrReplaceNode(head_node, atom.Title, title, true)
    }

    if ga_script != "" {
        insertOrReplaceNode(body_node, atom.Script, ga_script, false)
    }

    traverseAllNodes(body_node, func (node *html.Node) bool {
        return e.RewriteAttributes(node, css)
    })

    var b bytes.Buffer
    err3 := html.Render(&b, root)
    return b.String(), err3
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

func rewriteUrl(str string) string {
    if u, err := url.Parse(str); err == nil {
        if u.Host == "www.google.com" && u.Path == "/url" {
            q := u.Query().Get("q")
            if q != "" {
                return q
            }
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
        // to change the elements, explicitely access to the array items via reference.
        a := &node.Attr[index]

        if rewrite_urls && a.Namespace == "" && (a.Key == "href" || a.Key == "src") {
            a.Val = rewriteUrl(a.Val)
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
            // Keep the old value as a comment in the source
            result += "/* " + c.String(selector) + " */\n"
            if c.CleanupAttrs(e.Mode) {
                result += c.String(selector) + "\n"
            }
        }
    }

    return result, css, nil
}

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
            case //strings.HasPrefix(key, "border-"),
                //            strings.HasPrefix(key, "padding-"),
                //            strings.HasPrefix(key, "margin-"),
                key == "font-family" && strings.Contains(val, "Consolas"),
                key == "font-style" && val != "normal" && val != "inherit",
                key == "font-weight" && val != "normal" && val != "inherit",
                //            key == "font-size" && val != "inherit",
                //            key == "text-decoration" && val == "underline" && val != "inherit",
                key == "text-align" && val != "left" && val != "inherit",
                key == "color" && val != "#000000" && val != "inherit",
                key == "background-color" && val != "#ffffff" && val != "inherit":
                //            key == "max-width",
                //            key == "height",
                //            key == "line-height":
                continue
            }
            delete(*c, key)
        }
    }

    return len(*c) > 0
}
