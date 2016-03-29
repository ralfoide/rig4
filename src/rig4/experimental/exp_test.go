package experimental

import (
    "testing"
    "github.com/stretchr/testify/assert"
    "rig4/doc"
    "utils"
)

// -----

var EXP_TEST_MASTER_ID = "1iF14tncepmKpCIA4slb0TmKdkk3W7a58LoqG-MfihKo"
var EXP_TEST_FILE1_ID = "1HduRx12vE3aBDLBp4tbYEoab2UPRPdxmPy0oBDhXfc8"
var EXP_TEST_FILE2_ID = "1AaLSPRBTrrZtkvgtQ2VN5_Ooy25nDQNpbxx6dzDUPIU"

// -----

func TestExp_ReadFile(t *testing.T) {
    assert := assert.New(t)

    exp := expTestNewExp(t)
    doc_master := exp.ReadIndex(EXP_TEST_MASTER_ID)

    assert.Equal(expTestDataMaster(), doc_master.Content())
}

func TestExp_GetIndexEntries(t *testing.T) {
    assert := assert.New(t)

    exp := expTestNewExp(t)
    doc_master := doc.NewDocument("kind", "id", expTestDataMaster())
    entries := exp.GetIndexEntries(doc_master)

    assert.Equal(2, len(entries))

    assert.Equal("index.html", entries[0].DestName)
    assert.Equal(EXP_TEST_FILE1_ID, entries[0].DocId)

    assert.Equal("some_file1.html", entries[1].DestName)
    assert.Equal(EXP_TEST_FILE2_ID, entries[1].DocId)
}

func TestCss_ParseAttr(t *testing.T) {
    assert := assert.New(t)

    var c CssAttr
    selector := ".myclass"
    c = parseCssAttr(
        `background-color:#ffffff;max-width:468pt;padding:72pt 72pt 72pt 72pt;
        font-family:"Consolas";color:#1155cc;text-decoration:underline;
        font-size:11pt;border-right-style:solid;text-align:center;font-style:italic;
        font-weight:bold`)

    assert.Equal(
        `.myclass { background-color: #ffffff; border-right-style: solid; color: #1155cc; font-family: "Consolas"; font-size: 11pt; font-style: italic; font-weight: bold; max-width: 468pt; padding: 72pt 72pt 72pt 72pt; text-align: center; text-decoration: underline }`,
        c.String(selector))

    assert.True(c.CleanupAttrs(RewriteCss))
    assert.Equal(
        // v1: `.myclass { border-right-style: solid; color: #1155cc; font-family: "Consolas"; font-size: 11pt; font-style: italic; font-weight: bold; text-align: center; text-decoration: underline }`,
        // v2 with less attributes:
        `.myclass { color: #1155cc; font-family: "Consolas"; font-style: italic; font-weight: bold; text-align: center }`,
        c.String(selector))
}

func TestExp_SimplifyStyles(t *testing.T) {
    assert := assert.New(t)
    exp := expTestNewExp(t)
    original := `
@import url('https://themes.example.com/fonts/css?kit=lhDjYqiy3mZ0x6ROQEUoUw');
ol{margin:0;padding:0}
table td,table th{padding:0}
.c3{border-right-style:solid;padding:5pt 5pt 5pt 5pt;border-bottom-color:#a4c2f4;border-top-width:1pt;border-right-width:1pt;border-left-color:#a4c2f4;vertical-align:top;border-right-color:#a4c2f4;border-left-width:1pt;border-top-style:solid;background-color:#d0e0e3;border-left-style:solid;border-bottom-width:1pt;width:468pt;border-top-color:#a4c2f4;border-bottom-style:solid}
.c14{border-right-style:solid;padding:5pt 5pt 5pt 5pt;border-bottom-color:#a4c2f4;border-top-width:1pt;border-right-width:1pt;border-left-color:#a4c2f4;vertical-align:top;border-right-color:#a4c2f4;border-left-width:1pt;border-top-style:solid;background-color:#a4c2f4;border-left-style:solid;border-bottom-width:1pt;width:468pt;border-top-color:#a4c2f4;border-bottom-style:solid}
.c1{color:#000000;font-weight:normal;text-decoration:none;vertical-align:baseline;font-size:11pt;font-family:"Trebuchet MS";font-style:normal}
.c4{color:#000000;font-weight:bold;text-decoration:none;vertical-align:baseline;font-size:11pt;font-family:"Trebuchet MS";font-style:normal}
.c7{padding-top:0pt;padding-bottom:0pt;line-height:1.0;text-align:left;height:11pt}
.c9{padding-top:0pt;padding-bottom:0pt;line-height:1.15;text-align:left}
.c11{border-spacing:0;border-collapse:collapse;margin-right:auto}
.c10{padding-top:0pt;padding-bottom:0pt;line-height:1.15;text-align:center}
.c2{orphans:2;widows:2;height:11pt}
.c16{background-color:#ffffff;max-width:468pt;padding:72pt 72pt 72pt 72pt}
.c0{font-family:"Trebuchet MS";color:#1155cc;text-decoration:underline}
.c12{orphans:2;widows:2}
.c8{color:inherit;text-decoration:inherit}
.c13{page-break-after:avoid;text-align:center}
.c6{font-family:"Trebuchet MS"}
.c15{text-align:center}
.c5{height:0pt}
.title{padding-top:0pt;color:#000000;font-size:26pt;padding-bottom:3pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
.subtitle{padding-top:0pt;color:#666666;font-size:15pt;padding-bottom:16pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
li{color:#000000;font-size:11pt;font-family:"Arial"}
p{margin:0;color:#000000;font-size:11pt;font-family:"Arial"}
h1{padding-top:20pt;color:#000000;font-size:20pt;padding-bottom:6pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h2{padding-top:18pt;color:#000000;font-size:16pt;padding-bottom:6pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h3{padding-top:16pt;color:#434343;font-size:14pt;padding-bottom:4pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h4{padding-top:14pt;color:#666666;font-size:12pt;padding-bottom:4pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h5{padding-top:12pt;color:#666666;font-size:11pt;padding-bottom:4pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h6{padding-top:12pt;color:#666666;font-size:11pt;padding-bottom:4pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;font-style:italic;orphans:2;widows:2;text-align:left}`

    expected := `/* .c0 { color: #1155cc; font-family: "Trebuchet MS"; text-decoration: underline } */
.c0 { color: #1155cc }
/* .c1 { color: #000000; font-family: "Trebuchet MS"; font-size: 11pt; font-style: normal; font-weight: normal; text-decoration: none; vertical-align: baseline } */
/* .c10 { line-height: 1.15; padding-bottom: 0pt; padding-top: 0pt; text-align: center } */
.c10 { text-align: center }
/* .c11 { border-collapse: collapse; border-spacing: 0; margin-right: auto } */
/* .c12 { orphans: 2; widows: 2 } */
/* .c13 { page-break-after: avoid; text-align: center } */
.c13 { text-align: center }
/* .c14 { background-color: #a4c2f4; border-bottom-color: #a4c2f4; border-bottom-style: solid; border-bottom-width: 1pt; border-left-color: #a4c2f4; border-left-style: solid; border-left-width: 1pt; border-right-color: #a4c2f4; border-right-style: solid; border-right-width: 1pt; border-top-color: #a4c2f4; border-top-style: solid; border-top-width: 1pt; padding: 5pt 5pt 5pt 5pt; vertical-align: top; width: 468pt } */
.c14 { background-color: #a4c2f4 }
/* .c15 { text-align: center } */
.c15 { text-align: center }
/* .c16 { background-color: #ffffff; max-width: 468pt; padding: 72pt 72pt 72pt 72pt } */
/* .c2 { height: 11pt; orphans: 2; widows: 2 } */
/* .c3 { background-color: #d0e0e3; border-bottom-color: #a4c2f4; border-bottom-style: solid; border-bottom-width: 1pt; border-left-color: #a4c2f4; border-left-style: solid; border-left-width: 1pt; border-right-color: #a4c2f4; border-right-style: solid; border-right-width: 1pt; border-top-color: #a4c2f4; border-top-style: solid; border-top-width: 1pt; padding: 5pt 5pt 5pt 5pt; vertical-align: top; width: 468pt } */
.c3 { background-color: #d0e0e3 }
/* .c4 { color: #000000; font-family: "Trebuchet MS"; font-size: 11pt; font-style: normal; font-weight: bold; text-decoration: none; vertical-align: baseline } */
.c4 { font-weight: bold }
/* .c5 { height: 0pt } */
/* .c6 { font-family: "Trebuchet MS" } */
/* .c7 { height: 11pt; line-height: 1.0; padding-bottom: 0pt; padding-top: 0pt; text-align: left } */
/* .c8 { color: inherit; text-decoration: inherit } */
/* .c9 { line-height: 1.15; padding-bottom: 0pt; padding-top: 0pt; text-align: left } */
/* .subtitle { color: #666666; font-family: "Arial"; font-size: 15pt; line-height: 1.15; orphans: 2; padding-bottom: 16pt; padding-top: 0pt; page-break-after: avoid; text-align: left; widows: 2 } */
.subtitle { color: #666666 }
/* .title { color: #000000; font-family: "Arial"; font-size: 26pt; line-height: 1.15; orphans: 2; padding-bottom: 3pt; padding-top: 0pt; page-break-after: avoid; text-align: left; widows: 2 } */
`

    result, css, err := exp.SimplifyStyles(original, "c16")

    assert.Nil(err)
    assert.NotNil(css)
    assert.Equal(expected, result, utils.StringDiff(expected, result))
}

func TestExp_ProcessEntry1(t *testing.T) {
    assert := assert.New(t)

    exp := expTestNewExp(t)

    ga_script := `
for(i=0; i<10; i++) { print "script"; }
`

    index := exp.ReadHtml(EXP_TEST_FILE1_ID)
    result, err := exp.ProcessEntry(index.Content(), "My Title", ga_script)

    assert.Nil(err)
    assert.Equal(expTestResultFile1(), result, utils.StringDiff(expTestResultFile1(), result))

    //    file1 := exp.ReadHtml("1AaLSPRBTrrZtkvgtQ2VN5_Ooy25nDQNpbxx6dzDUPIU")
    //    assert.Equal(strings.Trim(expTestDataFile1(), "\n"), file1.Content())
}

func TestExp_RewriteUrl(t *testing.T) {
    assert := assert.New(t)

    e := expTestNewExp(t)

    assert.Equal("http://www.example.com",
        e.RewriteUrl("https://www.google.com/url?q=http://www.example.com&amp;sa=D&amp;ust=1455499447044000&amp;usg=AFQjCNFs3KTweWi-ktuTELf_0HC6UBXLpQ"))

    assert.Equal("http://www.example.com/index.html",
        e.RewriteUrl("https://www.google.com/url?q=http://www.example.com/index.html&amp;sa=D&amp;ust=1455499447047000&amp;usg=AFQjCNF4n60jdI_Pi72_Kj4fa9svhvp0_w"))

    assert.Equal("https://www.example.com/url?q=http://www.example.com/index.html&amp;what=ever",
        e.RewriteUrl("https://www.example.com/url?q=http://www.example.com/index.html&amp;what=ever"))

    assert.Equal("https://lh3.googleusercontent.com/xfS4pjf6g-Vb99nZKiK1Hf2aKJM61Agx2Sa1eM4kUmAVZ1HSzbAy1bheQYPQX-7fRGjd7vl5R0ItYChL4tyb8wUiphzdDBNjq1qjOzro9mDcJs90j71HbExtcEpNne9eIEW-88cu",
        e.RewriteUrl("https://lh3.googleusercontent.com/xfS4pjf6g-Vb99nZKiK1Hf2aKJM61Agx2Sa1eM4kUmAVZ1HSzbAy1bheQYPQX-7fRGjd7vl5R0ItYChL4tyb8wUiphzdDBNjq1qjOzro9mDcJs90j71HbExtcEpNne9eIEW-88cu"))

    assert.Equal("https://docs.google.com/drawings/image?id=s7Dyv_q6qR4PYd0tATI9Ucg&amp;rev=2&amp;h=120&amp;w=624&amp;ac=1",
        e.RewriteUrl("https://docs.google.com/drawings/image?id=s7Dyv_q6qR4PYd0tATI9Ucg&amp;rev=2&amp;h=120&amp;w=624&amp;ac=1"))

    assert.Equal("https://www.youtube.com/playlist?list=PLjmlvzL_NxLof_RzTo6kduzMx6MYt_EBj",
        e.RewriteUrl("https://www.google.com/url?q=https://www.youtube.com/playlist?list%3DPLjmlvzL_NxLof_RzTo6kduzMx6MYt_EBj&amp;sa=D&amp;ust=1455499447053000&amp;usg=AFQjCNEugJPtr0_akGnDjMrVDUimrXCXGA"))
}

// ----

// Implements IGDocReader
type MockGDocReader struct {
    T *testing.T
}


func (g *MockGDocReader) Init() error {
    return nil
}

func (g *MockGDocReader) Kind() string {
    return "kind"
}

func (g *MockGDocReader) ReadDocuments(docs doc.IDocuments, uri string) error {
    return nil
}

func (g *MockGDocReader) ReadFileById(id string, mimetype string) (doc.IDocument, error) {
    var content string
    switch id {
    case EXP_TEST_MASTER_ID:
        content = expTestDataMaster()
    case EXP_TEST_FILE1_ID:
        content = expTestDataFile1()
    case EXP_TEST_FILE2_ID:
        content = expTestDataFile2()
    default:
        assert.Fail(g.T, "ReadFileById, Invalid ID: " + id)
    }
    assert.NotEqual(g.T, "", content)

    d := doc.NewDocument("Kind", id, content)
    return d, nil
}

func (g *MockGDocReader) Get(url string) ([]byte, error) {
    return []byte{}, nil
}

func expTestNewExp(t *testing.T) *Exp {
    gd := &MockGDocReader{T: t}
    exp := &Exp{Reader: gd, Mode: RewriteUrls | RewriteCss}
    return exp
}

func expTestDataMaster() string {
    return "\ufeff# Index file for the Rig4 doc-based web site.\r\n# Note that the doc title is used as html title. GA script is added at the end automatically.\r\n# target path        id        comment\r\n\r\n\r\nindex.html        1HduRx12vE3aBDLBp4tbYEoab2UPRPdxmPy0oBDhXfc8\r\n\r\n\r\nsome_file1.html        1AaLSPRBTrrZtkvgtQ2VN5_Ooy25nDQNpbxx6dzDUPIU\r\n\r\n\r\n# end"
}

func expTestDataFile1() string {
    return `<html>
<head>
<meta content="text/html; charset=UTF-8" http-equiv="content-type">
<style type="text/css">ol{margin:0;padding:0}
table td,table th{padding:0}
.c3{border-right-style:solid;padding:5pt 5pt 5pt 5pt;border-bottom-color:#a4c2f4;border-top-width:1pt;border-right-width:1pt;border-left-color:#a4c2f4;vertical-align:top;border-right-color:#a4c2f4;border-left-width:1pt;border-top-style:solid;background-color:#d0e0e3;border-left-style:solid;border-bottom-width:1pt;width:468pt;border-top-color:#a4c2f4;border-bottom-style:solid}
.c14{border-right-style:solid;padding:5pt 5pt 5pt 5pt;border-bottom-color:#a4c2f4;border-top-width:1pt;border-right-width:1pt;border-left-color:#a4c2f4;vertical-align:top;border-right-color:#a4c2f4;border-left-width:1pt;border-top-style:solid;background-color:#a4c2f4;border-left-style:solid;border-bottom-width:1pt;width:468pt;border-top-color:#a4c2f4;border-bottom-style:solid}
.c1{color:#000000;font-weight:normal;text-decoration:none;vertical-align:baseline;font-size:11pt;font-family:"Trebuchet MS";font-style:normal}
.c4{color:#000000;font-weight:bold;text-decoration:none;vertical-align:baseline;font-size:11pt;font-family:"Trebuchet MS";font-style:normal}
.c7{padding-top:0pt;padding-bottom:0pt;line-height:1.0;text-align:left;height:11pt}
.c9{padding-top:0pt;padding-bottom:0pt;line-height:1.15;text-align:left}
.c11{border-spacing:0;border-collapse:collapse;margin-right:auto}
.c10{padding-top:0pt;padding-bottom:0pt;line-height:1.15;text-align:center}
.c2{orphans:2;widows:2;height:11pt}
.c16{background-color:#ffffff;max-width:468pt;padding:72pt 72pt 72pt 72pt}
.c0{font-family:"Trebuchet MS";color:#1155cc;text-decoration:underline}
.c12{orphans:2;widows:2}
.c8{color:inherit;text-decoration:inherit}
.c13{page-break-after:avoid;text-align:center}
.c6{font-family:"Trebuchet MS"}
.c15{text-align:center}
.c5{height:0pt}
.title{padding-top:0pt;color:#000000;font-size:26pt;padding-bottom:3pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
.subtitle{padding-top:0pt;color:#666666;font-size:15pt;padding-bottom:16pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
li{color:#000000;font-size:11pt;font-family:"Arial"}
p{margin:0;color:#000000;font-size:11pt;font-family:"Arial"}
h1{padding-top:20pt;color:#000000;font-size:20pt;padding-bottom:6pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h2{padding-top:18pt;color:#000000;font-size:16pt;padding-bottom:6pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h3{padding-top:16pt;color:#434343;font-size:14pt;padding-bottom:4pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h4{padding-top:14pt;color:#666666;font-size:12pt;padding-bottom:4pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h5{padding-top:12pt;color:#666666;font-size:11pt;padding-bottom:4pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h6{padding-top:12pt;color:#666666;font-size:11pt;padding-bottom:4pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;font-style:italic;orphans:2;widows:2;text-align:left}
</style>
</head>
<body class="c16">
<p class="c12 c13 title">
<a id="h.djkf3ocgpiiq">
</a>
<span class="c0">
<a class="c8" href="https://www.google.com/url?q=http://www.example.com&amp;sa=D&amp;ust=1455499447044000&amp;usg=AFQjCNFs3KTweWi-ktuTELf_0HC6UBXLpQ">Index Page</a>
</span>
</p>
<p class="c2">
<span class="c6">
</span>
</p>
<p class="c2">
<span class="c6">
</span>
</p>
<a id="t.c1c696c4a5afa1dda081d9eefab023cd3eba87ce">
</a>
<a id="t.0">
</a>
<table class="c11">
<tbody>
<tr class="c5">
<td class="c14" colspan="1" rowspan="1">
<p class="c10 c12">
<span class="c4">Articles </span>
</p>
</td>
</tr>
<tr class="c5">
<td class="c3" colspan="1" rowspan="1">
<p class="c7">
<span class="c1">
</span>
</p>
<p class="c10 c12">
<span class="c0">
<a class="c8" href="https://www.google.com/url?q=http://www.example.com/index.html&amp;sa=D&amp;ust=1455499447047000&amp;usg=AFQjCNF4n60jdI_Pi72_Kj4fa9svhvp0_w">My article #1</a>
</span>
<span class="c6">
<br>
</span>
<span style="overflow: hidden; display: inline-block; margin: 0.00px 0.00px; border: 0.00px solid #000000; transform: rotate(0.00rad) translateZ(0px); -webkit-transform: rotate(0.00rad) translateZ(0px); width: 147.00px; height: 94.00px;">
<img alt="" src="https://lh3.googleusercontent.com/xfS4pjf6g-Vb99nZKiK1Hf2aKJM61Agx2Sa1eM4kUmAVZ1HSzbAy1bheQYPQX-7fRGjd7vl5R0ItYChL4tyb8wUiphzdDBNjq1qjOzro9mDcJs90j71HbExtcEpNne9eIEW-88cu" style="width: 147.00px; height: 94.00px; margin-left: 0.00px; margin-top: 0.00px;transform: rotate(0.00rad) translateZ(0px); -webkit-transform: rotate(0.00rad) translateZ(0px);" title="">
</span>
</p>
</td>
</tr>
<tr class="c5">
<td class="c3" colspan="1" rowspan="1">
<p class="c10 c12">
<span class="c0">
<a class="c8" href="https://www.google.com/url?q=http://www.example.com/index.html&amp;sa=D&amp;ust=1455499447049000&amp;usg=AFQjCNHOAD76-kI3QiYFTKjshJQeiL0iAA">Myarticle #1</a>
</span>
<span class="c6">
<br>
</span>
<span style="overflow: hidden; display: inline-block; margin: 0.00px 0.00px; border: 0.00px solid #000000; transform: rotate(0.00rad) translateZ(0px); -webkit-transform: rotate(0.00rad) translateZ(0px); width: 147.00px; height: 94.00px;">
<img alt="" src="https://lh3.googleusercontent.com/xfS4pjf6g-Vb99nZKiK1Hf2aKJM61Agx2Sa1eM4kUmAVZ1HSzbAy1bheQYPQX-7fRGjd7vl5R0ItYChL4tyb8wUiphzdDBNjq1qjOzro9mDcJs90j71HbExtcEpNne9eIEW-88cu" style="width: 147.00px; height: 94.00px; margin-left: 0.00px; margin-top: 0.00px; transform: rotate(0.00rad) translateZ(0px); -webkit-transform: rotate(0.00rad) translateZ(0px);" title="">
</span>
</p>
<p class="c2 c10">
<span class="c1">
</span>
</p>
</td>
</tr>
<tr class="c5">
<td class="c3" colspan="1" rowspan="1">
<p class="c10 c2">
<span class="c1">
</span>
</p>
<p class="c10 c2">
<span class="c1">
</span>
</p>
</td>
</tr>
<tr class="c5">
<td class="c14" colspan="1" rowspan="1">
<p class="c10 c12">
<span class="c4">Videos</span>
</p>
</td>
</tr>
<tr class="c5">
<td class="c3" colspan="1" rowspan="1">
<p class="c10 c2">
<span class="c1">
</span>
</p>
<p class="c10 c12">
<span class="c0">
<a class="c8" href="https://www.google.com/url?q=https://www.youtube.com/playlist?list%3DPLjmlvzL_NxLof_RzTo6kduzMx6MYt_EBj&amp;sa=D&amp;ust=1455499447053000&amp;usg=AFQjCNEugJPtr0_akGnDjMrVDUimrXCXGA">Link to a YouTube Playlist</a>
</span>
</p>
<p class="c10 c12">
<span class="c1">Description for the link to a YouTube Playlist.</span>
</p>
<p class="c10 c2">
<span class="c1">
</span>
</p>
<p class="c2 c9">
<span class="c1">
</span>
</p>
<p class="c9 c2">
<span class="c1">
</span>
</p>
</td>
</tr>
</tbody>
</table>
<p class="c2">
<span class="c6">
</span>
</p>
<p class="c2 c15">
<span class="c6">
</span>
</p>
<p class="c2">
<span>
</span>
</p>
<script>Any existing script is not replaced</script>
</body>
</html>`
}


func expTestResultFile1() string {
    return `<html><head>
<meta content="text/html; charset=UTF-8" http-equiv="content-type"/>
<style type="text/css">/* .c0 { color: #1155cc; font-family: "Trebuchet MS"; text-decoration: underline } */
.c0 { color: #1155cc }
/* .c1 { color: #000000; font-family: "Trebuchet MS"; font-size: 11pt; font-style: normal; font-weight: normal; text-decoration: none; vertical-align: baseline } */
/* .c10 { line-height: 1.15; padding-bottom: 0pt; padding-top: 0pt; text-align: center } */
.c10 { text-align: center }
/* .c11 { border-collapse: collapse; border-spacing: 0; margin-right: auto } */
/* .c12 { orphans: 2; widows: 2 } */
/* .c13 { page-break-after: avoid; text-align: center } */
.c13 { text-align: center }
/* .c14 { background-color: #a4c2f4; border-bottom-color: #a4c2f4; border-bottom-style: solid; border-bottom-width: 1pt; border-left-color: #a4c2f4; border-left-style: solid; border-left-width: 1pt; border-right-color: #a4c2f4; border-right-style: solid; border-right-width: 1pt; border-top-color: #a4c2f4; border-top-style: solid; border-top-width: 1pt; padding: 5pt 5pt 5pt 5pt; vertical-align: top; width: 468pt } */
.c14 { background-color: #a4c2f4 }
/* .c15 { text-align: center } */
.c15 { text-align: center }
/* .c16 { background-color: #ffffff; max-width: 468pt; padding: 72pt 72pt 72pt 72pt } */
/* .c2 { height: 11pt; orphans: 2; widows: 2 } */
/* .c3 { background-color: #d0e0e3; border-bottom-color: #a4c2f4; border-bottom-style: solid; border-bottom-width: 1pt; border-left-color: #a4c2f4; border-left-style: solid; border-left-width: 1pt; border-right-color: #a4c2f4; border-right-style: solid; border-right-width: 1pt; border-top-color: #a4c2f4; border-top-style: solid; border-top-width: 1pt; padding: 5pt 5pt 5pt 5pt; vertical-align: top; width: 468pt } */
.c3 { background-color: #d0e0e3 }
/* .c4 { color: #000000; font-family: "Trebuchet MS"; font-size: 11pt; font-style: normal; font-weight: bold; text-decoration: none; vertical-align: baseline } */
.c4 { font-weight: bold }
/* .c5 { height: 0pt } */
/* .c6 { font-family: "Trebuchet MS" } */
/* .c7 { height: 11pt; line-height: 1.0; padding-bottom: 0pt; padding-top: 0pt; text-align: left } */
/* .c8 { color: inherit; text-decoration: inherit } */
/* .c9 { line-height: 1.15; padding-bottom: 0pt; padding-top: 0pt; text-align: left } */
/* .subtitle { color: #666666; font-family: "Arial"; font-size: 15pt; line-height: 1.15; orphans: 2; padding-bottom: 16pt; padding-top: 0pt; page-break-after: avoid; text-align: left; widows: 2 } */
.subtitle { color: #666666 }
/* .title { color: #000000; font-family: "Arial"; font-size: 26pt; line-height: 1.15; orphans: 2; padding-bottom: 3pt; padding-top: 0pt; page-break-after: avoid; text-align: left; widows: 2 } */
</style>
<title>My Title</title></head>
<body class="c16">
<p class="c12 c13 title">
<a id="h.djkf3ocgpiiq">
</a>
<span class="c0">
<a class="c8" href="http://www.example.com">Index Page</a>
</span>
</p>
<p class="c2">
<span class="c6">
</span>
</p>
<p class="c2">
<span class="c6">
</span>
</p>
<a id="t.c1c696c4a5afa1dda081d9eefab023cd3eba87ce">
</a>
<a id="t.0">
</a>
<table class="c11">
<tbody>
<tr class="c5">
<td class="c14" colspan="1" rowspan="1">
<p class="c10 c12">
<span class="c4">Articles </span>
</p>
</td>
</tr>
<tr class="c5">
<td class="c3" colspan="1" rowspan="1">
<p class="c7">
<span class="c1">
</span>
</p>
<p class="c10 c12">
<span class="c0">
<a class="c8" href="http://www.example.com/index.html">My article #1</a>
</span>
<span class="c6">
<br/>
</span>
<span style="overflow: hidden; display: inline-block; margin: 0.00px 0.00px; border: 0.00px solid #000000; transform: rotate(0.00rad) translateZ(0px); -webkit-transform: rotate(0.00rad) translateZ(0px); width: 147.00px; height: 94.00px;">
<img alt="" src="https://lh3.googleusercontent.com/xfS4pjf6g-Vb99nZKiK1Hf2aKJM61Agx2Sa1eM4kUmAVZ1HSzbAy1bheQYPQX-7fRGjd7vl5R0ItYChL4tyb8wUiphzdDBNjq1qjOzro9mDcJs90j71HbExtcEpNne9eIEW-88cu" style="width: 147.00px; height: 94.00px; margin-left: 0.00px; margin-top: 0.00px;transform: rotate(0.00rad) translateZ(0px); -webkit-transform: rotate(0.00rad) translateZ(0px);" title=""/>
</span>
</p>
</td>
</tr>
<tr class="c5">
<td class="c3" colspan="1" rowspan="1">
<p class="c10 c12">
<span class="c0">
<a class="c8" href="http://www.example.com/index.html">Myarticle #1</a>
</span>
<span class="c6">
<br/>
</span>
<span style="overflow: hidden; display: inline-block; margin: 0.00px 0.00px; border: 0.00px solid #000000; transform: rotate(0.00rad) translateZ(0px); -webkit-transform: rotate(0.00rad) translateZ(0px); width: 147.00px; height: 94.00px;">
<img alt="" src="https://lh3.googleusercontent.com/xfS4pjf6g-Vb99nZKiK1Hf2aKJM61Agx2Sa1eM4kUmAVZ1HSzbAy1bheQYPQX-7fRGjd7vl5R0ItYChL4tyb8wUiphzdDBNjq1qjOzro9mDcJs90j71HbExtcEpNne9eIEW-88cu" style="width: 147.00px; height: 94.00px; margin-left: 0.00px; margin-top: 0.00px; transform: rotate(0.00rad) translateZ(0px); -webkit-transform: rotate(0.00rad) translateZ(0px);" title=""/>
</span>
</p>
<p class="c2 c10">
<span class="c1">
</span>
</p>
</td>
</tr>
<tr class="c5">
<td class="c3" colspan="1" rowspan="1">
<p class="c10 c2">
<span class="c1">
</span>
</p>
<p class="c10 c2">
<span class="c1">
</span>
</p>
</td>
</tr>
<tr class="c5">
<td class="c14" colspan="1" rowspan="1">
<p class="c10 c12">
<span class="c4">Videos</span>
</p>
</td>
</tr>
<tr class="c5">
<td class="c3" colspan="1" rowspan="1">
<p class="c10 c2">
<span class="c1">
</span>
</p>
<p class="c10 c12">
<span class="c0">
<a class="c8" href="https://www.youtube.com/playlist?list=PLjmlvzL_NxLof_RzTo6kduzMx6MYt_EBj">Link to a YouTube Playlist</a>
</span>
</p>
<p class="c10 c12">
<span class="c1">Description for the link to a YouTube Playlist.</span>
</p>
<p class="c10 c2">
<span class="c1">
</span>
</p>
<p class="c2 c9">
<span class="c1">
</span>
</p>
<p class="c9 c2">
<span class="c1">
</span>
</p>
</td>
</tr>
</tbody>
</table>
<p class="c2">
<span class="c6">
</span>
</p>
<p class="c2 c15">
<span class="c6">
</span>
</p>
<p class="c2">
<span>
</span>
</p>
<script>Any existing script is not replaced</script>

<script>
for(i=0; i<10; i++) { print "script"; }
</script></body></html>`
}

func expTestDataFile2() string {
    return `<html>
<head>
<meta content="text/html; charset=UTF-8" http-equiv="content-type">
<style type="text/css">@import url('https://themes.googleusercontent.com/fonts/css?kit=lhDjYqiy3mZ0x6ROQEUoUw');
ul.lst-kix_lzsps1nc0z3a-2{list-style-type:none}
ul.lst-kix_w23fluu06u24-6{list-style-type:none}
ul.lst-kix_lzsps1nc0z3a-1{list-style-type:none}
ul.lst-kix_w23fluu06u24-7{list-style-type:none}
ul.lst-kix_lzsps1nc0z3a-0{list-style-type:none}
ul.lst-kix_w23fluu06u24-8{list-style-type:none}
.lst-kix_6ypghliz41jd-8>li:before{content:"\0025a0  "}
ul.lst-kix_lzsps1nc0z3a-8{list-style-type:none}
ul.lst-kix_w23fluu06u24-0{list-style-type:none}
ul.lst-kix_lzsps1nc0z3a-7{list-style-type:none}
ul.lst-kix_w23fluu06u24-1{list-style-type:none}
.lst-kix_6ypghliz41jd-7>li:before{content:"\0025cb  "}
ul.lst-kix_lzsps1nc0z3a-6{list-style-type:none}
ul.lst-kix_w23fluu06u24-2{list-style-type:none}
ul.lst-kix_lzsps1nc0z3a-5{list-style-type:none}
ul.lst-kix_w23fluu06u24-3{list-style-type:none}
ul.lst-kix_lzsps1nc0z3a-4{list-style-type:none}
ul.lst-kix_w23fluu06u24-4{list-style-type:none}
ul.lst-kix_lzsps1nc0z3a-3{list-style-type:none}
ul.lst-kix_w23fluu06u24-5{list-style-type:none}
.lst-kix_6ypghliz41jd-5>li:before{content:"\0025a0  "}
ul.lst-kix_44ikc98nvgs1-1{list-style-type:none}
ul.lst-kix_44ikc98nvgs1-0{list-style-type:none}
.lst-kix_6ypghliz41jd-4>li:before{content:"\0025cb  "}
.lst-kix_6ypghliz41jd-6>li:before{content:"\0025cf  "}
ul.lst-kix_44ikc98nvgs1-3{list-style-type:none}
ul.lst-kix_44ikc98nvgs1-2{list-style-type:none}
.lst-kix_lzsps1nc0z3a-0>li:before{content:"\0025cf  "}
.lst-kix_6ypghliz41jd-2>li:before{content:"\0025a0  "}
.lst-kix_6ypghliz41jd-3>li:before{content:"\0025cf  "}
.lst-kix_lzsps1nc0z3a-3>li:before{content:"\0025cf  "}
.lst-kix_w0lvunv3z7mg-0>li:before{content:"\0025cf  "}
.lst-kix_w0lvunv3z7mg-1>li:before{content:"\0025cb  "}
.lst-kix_lzsps1nc0z3a-1>li:before{content:"\0025cb  "}
.lst-kix_lzsps1nc0z3a-5>li:before{content:"\0025a0  "}
.lst-kix_w0lvunv3z7mg-2>li:before{content:"\0025a0  "}
.lst-kix_lzsps1nc0z3a-4>li:before{content:"\0025cb  "}
.lst-kix_w0lvunv3z7mg-4>li:before{content:"\0025cb  "}
.lst-kix_w0lvunv3z7mg-3>li:before{content:"\0025cf  "}
.lst-kix_w0lvunv3z7mg-5>li:before{content:"\0025a0  "}
.lst-kix_lzsps1nc0z3a-2>li:before{content:"\0025a0  "}
.lst-kix_w0lvunv3z7mg-8>li:before{content:"\0025a0  "}
.lst-kix_w0lvunv3z7mg-7>li:before{content:"\0025cb  "}
.lst-kix_w0lvunv3z7mg-6>li:before{content:"\0025cf  "}
.lst-kix_lzsps1nc0z3a-8>li:before{content:"\0025a0  "}
.lst-kix_lzsps1nc0z3a-7>li:before{content:"\0025cb  "}
.lst-kix_lzsps1nc0z3a-6>li:before{content:"\0025cf  "}
.lst-kix_lpjtg05snqzg-8>li:before{content:"\0025a0  "}
ul.lst-kix_lpjtg05snqzg-2{list-style-type:none}
ul.lst-kix_lpjtg05snqzg-3{list-style-type:none}
ul.lst-kix_kp6zj2ygenwx-8{list-style-type:none}
ul.lst-kix_lpjtg05snqzg-0{list-style-type:none}
ul.lst-kix_lpjtg05snqzg-1{list-style-type:none}
.lst-kix_lpjtg05snqzg-5>li:before{content:"\0025a0  "}
.lst-kix_lpjtg05snqzg-7>li:before{content:"\0025cb  "}
.lst-kix_w23fluu06u24-4>li:before{content:"\0025cb  "}
.lst-kix_w23fluu06u24-6>li:before{content:"\0025cf  "}
.lst-kix_lpjtg05snqzg-2>li:before{content:"\0025a0  "}
.lst-kix_lpjtg05snqzg-6>li:before{content:"\0025cf  "}
.lst-kix_w23fluu06u24-3>li:before{content:"\0025cf  "}
.lst-kix_w23fluu06u24-7>li:before{content:"\0025cb  "}
ul.lst-kix_lpjtg05snqzg-8{list-style-type:none}
.lst-kix_lpjtg05snqzg-3>li:before{content:"\0025cf  "}
ul.lst-kix_lpjtg05snqzg-6{list-style-type:none}
ul.lst-kix_lpjtg05snqzg-7{list-style-type:none}
ul.lst-kix_lpjtg05snqzg-4{list-style-type:none}
.lst-kix_lpjtg05snqzg-4>li:before{content:"\0025cb  "}
ul.lst-kix_lpjtg05snqzg-5{list-style-type:none}
.lst-kix_w23fluu06u24-5>li:before{content:"\0025a0  "}
ul.lst-kix_kp6zj2ygenwx-2{list-style-type:none}
ul.lst-kix_kp6zj2ygenwx-3{list-style-type:none}
ul.lst-kix_kp6zj2ygenwx-0{list-style-type:none}
ul.lst-kix_kp6zj2ygenwx-1{list-style-type:none}
.lst-kix_lpjtg05snqzg-1>li:before{content:"\0025cb  "}
.lst-kix_w23fluu06u24-8>li:before{content:"\0025a0  "}
ul.lst-kix_kp6zj2ygenwx-6{list-style-type:none}
.lst-kix_lpjtg05snqzg-0>li:before{content:"\0025cf  "}
ul.lst-kix_kp6zj2ygenwx-7{list-style-type:none}
ul.lst-kix_kp6zj2ygenwx-4{list-style-type:none}
ul.lst-kix_kp6zj2ygenwx-5{list-style-type:none}
ul.lst-kix_w0lvunv3z7mg-1{list-style-type:none}
ul.lst-kix_w0lvunv3z7mg-2{list-style-type:none}
ul.lst-kix_w0lvunv3z7mg-3{list-style-type:none}
ul.lst-kix_w0lvunv3z7mg-4{list-style-type:none}
ul.lst-kix_w0lvunv3z7mg-5{list-style-type:none}
ul.lst-kix_w0lvunv3z7mg-6{list-style-type:none}
ul.lst-kix_w0lvunv3z7mg-7{list-style-type:none}
ul.lst-kix_w0lvunv3z7mg-8{list-style-type:none}
.lst-kix_6ypghliz41jd-1>li:before{content:"\0025cb  "}
ul.lst-kix_44ikc98nvgs1-8{list-style-type:none}
.lst-kix_6ypghliz41jd-0>li:before{content:"\0025cf  "}
ul.lst-kix_44ikc98nvgs1-5{list-style-type:none}
ul.lst-kix_44ikc98nvgs1-4{list-style-type:none}
ul.lst-kix_44ikc98nvgs1-7{list-style-type:none}
ul.lst-kix_44ikc98nvgs1-6{list-style-type:none}
ul.lst-kix_w0lvunv3z7mg-0{list-style-type:none}
.lst-kix_kp6zj2ygenwx-6>li:before{content:"\0025cf  "}
.lst-kix_kp6zj2ygenwx-8>li:before{content:"\0025a0  "}
.lst-kix_kp6zj2ygenwx-5>li:before{content:"\0025a0  "}
.lst-kix_kp6zj2ygenwx-2>li:before{content:"\0025a0  "}
.lst-kix_kp6zj2ygenwx-4>li:before{content:"\0025cb  "}
.lst-kix_kp6zj2ygenwx-3>li:before{content:"\0025cf  "}
ul.lst-kix_6ypghliz41jd-6{list-style-type:none}
ul.lst-kix_6ypghliz41jd-5{list-style-type:none}
ul.lst-kix_6ypghliz41jd-4{list-style-type:none}
ul.lst-kix_6ypghliz41jd-3{list-style-type:none}
.lst-kix_kp6zj2ygenwx-0>li:before{content:"\0025cf  "}
ul.lst-kix_6ypghliz41jd-8{list-style-type:none}
ul.lst-kix_6ypghliz41jd-7{list-style-type:none}
.lst-kix_kp6zj2ygenwx-1>li:before{content:"\0025cb  "}
ul.lst-kix_6ypghliz41jd-2{list-style-type:none}
ul.lst-kix_6ypghliz41jd-1{list-style-type:none}
ul.lst-kix_6ypghliz41jd-0{list-style-type:none}
.lst-kix_h54eep7opryi-0>li:before{content:"\0025cf  "}
.lst-kix_h54eep7opryi-3>li:before{content:"\0025cf  "}
.lst-kix_h54eep7opryi-1>li:before{content:"\0025cb  "}
.lst-kix_h54eep7opryi-2>li:before{content:"\0025a0  "}
.lst-kix_an3dwlm52ks6-2>li:before{content:"-  "}
ul.lst-kix_h54eep7opryi-5{list-style-type:none}
.lst-kix_h54eep7opryi-7>li:before{content:"\0025cb  "}
ul.lst-kix_h54eep7opryi-4{list-style-type:none}
ul.lst-kix_h54eep7opryi-3{list-style-type:none}
ul.lst-kix_h54eep7opryi-2{list-style-type:none}
.lst-kix_an3dwlm52ks6-0>li:before{content:"-  "}
.lst-kix_an3dwlm52ks6-4>li:before{content:"-  "}
ul.lst-kix_h54eep7opryi-1{list-style-type:none}
ul.lst-kix_h54eep7opryi-0{list-style-type:none}
.lst-kix_an3dwlm52ks6-3>li:before{content:"-  "}
.lst-kix_h54eep7opryi-4>li:before{content:"\0025cb  "}
.lst-kix_h54eep7opryi-8>li:before{content:"\0025a0  "}
.lst-kix_w23fluu06u24-0>li:before{content:"\0025cf  "}
.lst-kix_w23fluu06u24-2>li:before{content:"\0025a0  "}
.lst-kix_h54eep7opryi-5>li:before{content:"\0025a0  "}
ul.lst-kix_h54eep7opryi-8{list-style-type:none}
.lst-kix_an3dwlm52ks6-1>li:before{content:"-  "}
.lst-kix_h54eep7opryi-6>li:before{content:"\0025cf  "}
ul.lst-kix_h54eep7opryi-7{list-style-type:none}
ul.lst-kix_h54eep7opryi-6{list-style-type:none}
.lst-kix_w23fluu06u24-1>li:before{content:"\0025cb  "}
.lst-kix_an3dwlm52ks6-8>li:before{content:"-  "}
.lst-kix_an3dwlm52ks6-7>li:before{content:"-  "}
.lst-kix_an3dwlm52ks6-6>li:before{content:"-  "}
.lst-kix_44ikc98nvgs1-3>li:before{content:"\0025cf  "}
.lst-kix_44ikc98nvgs1-4>li:before{content:"\0025cb  "}
.lst-kix_44ikc98nvgs1-5>li:before{content:"\0025a0  "}
.lst-kix_an3dwlm52ks6-5>li:before{content:"-  "}
.lst-kix_44ikc98nvgs1-2>li:before{content:"\0025a0  "}
.lst-kix_44ikc98nvgs1-0>li:before{content:"\0025cf  "}
.lst-kix_44ikc98nvgs1-1>li:before{content:"\0025cb  "}
ul.lst-kix_an3dwlm52ks6-2{list-style-type:none}
ul.lst-kix_an3dwlm52ks6-3{list-style-type:none}
.lst-kix_44ikc98nvgs1-6>li:before{content:"\0025cf  "}
.lst-kix_44ikc98nvgs1-7>li:before{content:"\0025cb  "}
ul.lst-kix_an3dwlm52ks6-4{list-style-type:none}
ul.lst-kix_an3dwlm52ks6-5{list-style-type:none}
ul.lst-kix_an3dwlm52ks6-6{list-style-type:none}
ul.lst-kix_an3dwlm52ks6-7{list-style-type:none}
ul.lst-kix_an3dwlm52ks6-8{list-style-type:none}
.lst-kix_44ikc98nvgs1-8>li:before{content:"\0025a0  "}
ul.lst-kix_an3dwlm52ks6-0{list-style-type:none}
ul.lst-kix_an3dwlm52ks6-1{list-style-type:none}
.lst-kix_kp6zj2ygenwx-7>li:before{content:"\0025cb  "}
ol{margin:0;padding:0}
table td,table th{padding:0}
.c6{border-right-style:solid;padding:5pt 5pt 5pt 5pt;border-bottom-color:#000000;border-top-width:1pt;border-right-width:1pt;border-left-color:#000000;vertical-align:top;border-right-color:#000000;border-left-width:1pt;border-top-style:solid;background-color:#fce5cd;border-left-style:solid;border-bottom-width:1pt;width:468pt;border-top-color:#000000;border-bottom-style:solid}
.c21{border-right-style:solid;padding:5pt 5pt 5pt 5pt;border-bottom-color:#a4c2f4;border-top-width:1pt;border-right-width:1pt;border-left-color:#a4c2f4;vertical-align:top;border-right-color:#a4c2f4;border-left-width:1pt;border-top-style:solid;background-color:#d0e0e3;border-left-style:solid;border-bottom-width:1pt;width:468pt;border-top-color:#a4c2f4;border-bottom-style:solid}
.c19{color:#000000;font-weight:normal;text-decoration:none;vertical-align:baseline;font-size:11pt;font-family:"Consolas";font-style:normal}
.c0{color:#000000;font-weight:bold;text-decoration:none;vertical-align:baseline;font-size:24pt;font-family:"Arial";font-style:normal}
.c16{padding-top:0pt;padding-bottom:0pt;line-height:1.0;text-align:left}
.c5{padding-top:0pt;padding-bottom:0pt;line-height:1.0;text-align:center}
.c12{margin-left:auto;border-spacing:0;border-collapse:collapse;margin-right:auto}
.c18{border-spacing:0;border-collapse:collapse;margin-right:auto}
.c15{background-color:#ffffff;max-width:468pt;padding:72pt 72pt 72pt 72pt}
.c8{color:#1155cc;text-decoration:underline}
.c1{orphans:2;widows:2}
.c9{line-height:1.0;text-align:center}
.c11{color:inherit;text-decoration:inherit}
.c20{page-break-after:avoid}
.c4{font-family:"Trebuchet MS"}
.c2{font-weight:bold}
.c13{text-align:center}
.c14{font-size:24pt}
.c7{height:0pt}
.c10{font-style:italic}
.c3{height:11pt}
.c17{line-height:1.0}
.title{padding-top:0pt;color:#000000;font-size:26pt;padding-bottom:3pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
.subtitle{padding-top:0pt;color:#666666;font-size:15pt;padding-bottom:16pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
li{color:#000000;font-size:11pt;font-family:"Arial"}
p{margin:0;color:#000000;font-size:11pt;font-family:"Arial"}
h1{padding-top:20pt;color:#000000;font-size:20pt;padding-bottom:6pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h2{padding-top:18pt;color:#000000;font-size:16pt;padding-bottom:6pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h3{padding-top:16pt;color:#434343;font-size:14pt;padding-bottom:4pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h4{padding-top:14pt;color:#666666;font-size:12pt;padding-bottom:4pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h5{padding-top:12pt;color:#666666;font-size:11pt;padding-bottom:4pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;orphans:2;widows:2;text-align:left}
h6{padding-top:12pt;color:#666666;font-size:11pt;padding-bottom:4pt;font-family:"Arial";line-height:1.15;page-break-after:avoid;font-style:italic;orphans:2;widows:2;text-align:left}
</style>
</head>
<body class="c15">
<p class="c1 c3 c9">
<span class="c4">
</span>
</p>
<a id="t.8347973ec3e75d61f5e733969b80fcaf6a60a1cc">
</a>
<a id="t.0">
</a>
<table class="c12">
<tbody>
<tr class="c7">
<td class="c21"colspan="1" rowspan="1">
<p class="c5">
<span class="c2 c14">Article Title</span>
</p>
</td>
</tr>
</tbody>
</table>
<p class="c1 c9 c3">
<span class="c4">
</span>
</p>
<p class="c1 c13">
<span>Date</span>
</p>
<p class="c1 c3">
<span>
</span>
</p>
<p class="c1">
<span>Lorem ipsum dolor sit amet, fabulas temporibus pri ea, nec veri aliquando prodesset te.</span>
</p>
<h1 class="c1 c20">
<a id="h.xxgbrvvkw4q5">
</a>
<span>Title 1</span>
</h1>
<p class="c1">
<span class="c8">
<a class="c11" href="https://www.google.com/url?q=http://www.example.com&amp;sa=D&amp;ust=1455503949011000&amp;usg=AFQjCNFGBrApUg7venIQSWjvW2beqI70AQ">Link</a>
</span>
<span>&nbsp;</span>
</p>
<p class="c1">
<span class="c2">Bold</span>
<span>.</span>
</p>
<p class="c1">
<span class="c10">Italics</span>
<span>.</span>
</p>
<p class="c1">
<span>Table with color and consolas:</span>
</p>
<p class="c1 c3">
<span>
</span>
</p>
<a id="t.33b63939a4f9f463731190bae6b1fe3825a194fa">
</a>
<aid="t.1">
</a>
<table class="c18">
<tbody>
<tr class="c7">
<td class="c6" colspan="1" rowspan="1">
<p class="c16">
<span class="c19">Lorem ipsum dolorsit amet, fabulas temporibus pri ea, nec veri aliquando prodesset te.</span>
</p>
</td>
</tr>
</tbody>
</table>
<p class="c1 c3">
<span>
</span>
</p>
<p class="c1">
<span>Image:</span>
</p>
<p class="c1">
<span style="overflow: hidden; display: inline-block; margin: 0.00px 0.00px; border: 0.00px solid #000000; transform: rotate(0.00rad) translateZ(0px); -webkit-transform: rotate(0.00rad) translateZ(0px); width: 518.00px; height: 275.00px;">
<img alt="" src="https://lh4.googleusercontent.com/hdQnGjtoyZFK4IjOMAXwbzZ_--pyFkBJMJmCYJdpiFUSfzRlU0XaWmdAHhdF1SSQcMFWa-vLLLUDZvnBGDuxwai4cGAcY_eWDe8f-Rka8WsEE1jQK3_4_MrL1CMrw8-M4RnwcJlr" style="width: 518.00px; height: 275.00px; margin-left: 0.00px; margin-top: 0.00px; transform: rotate(0.00rad) translateZ(0px); -webkit-transform: rotate(0.00rad) translateZ(0px);" title="">
</span>
</p>
<p class="c1 c3">
<span>
</span>
</p>
<p class="c1">
<span>Drawing:</span>
</p>
<p class="c1 c3">
<span>
</span>
</p>
<p class="c1">
<span style="overflow: hidden; display: inline-block; margin: 0.00px 0.00px; border: 0.00px solid #000000; transform: rotate(0.00rad) translateZ(0px); -webkit-transform: rotate(0.00rad) translateZ(0px); width: 624.00px; height: 120.00px;">
<img alt="" src="https://docs.google.com/drawings/image?id=s7Dyv_q6qR4PYd0tATI9Ucg&amp;rev=2&amp;h=120&amp;w=624&amp;ac=1" style="width: 624.00px; height: 120.00px; margin-left: 0.00px; margin-top: 0.00px; transform: rotate(0.00rad) translateZ(0px); -webkit-transform: rotate(0.00rad) translateZ(0px);" title="">
</span>
</p>
<p class="c1 c3">
<span>
</span>
</p>
<p class="c1 c3">
<span>
</span>
</p>
<p class="c1 c17">
<span class="c4">~~</span>
</p>
<p class="c1 c17">
<span class="c4">[</span>
<span class="c4 c8">
<a class="c11" href="https://www.google.com/url?q=http://www.example.com&amp;sa=D&amp;ust=1455503949016000&amp;usg=AFQjCNEe96bvg76uAFpFAkwnoKMzO4jg1Q">Back to main page</a>
</span>
<span class="c4">]</span>
</p>
<p class="c1 c17">
<span class="c4">~~</span>
</p>
<p class="c1 c3">
<span>
</span>
</p>
<p class="c1 c3">
<span>
</span>
</p>
</body>
</html>`
}
