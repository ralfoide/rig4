# Rig4 #


## Description

**Rig4** is a static site generator.

It's main purpose is to generate a site that contains both blog-like streams of
posts and standalone articles. 


## Configuration 

Configuration describes a number of sources. A source is a type (a.k.a. kind) and
a parameter specific to that source. A *source* indicates both *how* to get the data
but not *what* to do with it.

The canonical format handled by Rig4 is the Izumi text-based format.
An Izumi file can represent:

- Either a single *article*,
- Or a blog-like stream of *posts*.

The Izumi file starts with a header that defines a series of *Izumi tags*.
These tags indicate *what* to do with the document.
For example tags convey the tile, date and category of an article
or how a the various posts of a blog should be ordered.

In an Izumi blog, each posts also carries an Izumi header with tags
specific to that blog post such as its title and date.


### Configuration File

All parameters can be given in the configuration file.
Default config file is `~/.rig4rc`

Some parameters can also be given on the command-line.
When parameters are defined in both, *the ones from the configuration file prevail*.

The configuration file is a simple text file where each line is a variable definition.
The syntax is: `variable = value` and the value is taken till the end of the line.
There is no support for multi-line values or escaping.


### Sources

There are currently 2 kind of sources implemented. Each source takes one parameter
known as the *URI* which defines what the source should read:

- `file` source. Data is read from a file. The syntax is `file:/local/path/`.
- `gdoc` source. Data is read from Google Drive using the Drive API v2.
  The syntax is `gdoc:"the query you'd use in Google Drive"`.

Sources are configured via the `sources` command-line argument or the
similarly named `sources` configuration line.

The `sources` syntax is a comma-separated list of entries, where each entry is either:

- A source using the unquoted format `kind : uri` or quoted as `kind : "uri"`,
- A reference to another configuration variable to read.

An unquoted URI is parsed til the end of the line or the first comma separator.
A quoted URI is parsed til the next double-quote.

Here's an example of sources defined in a configuration file:

    sources = source1, source2
    source1 = file:/var/local/content/myblog.izu, more_izu_files
    source2 = gdoc:"title contains '[izumi]' and fullText contains '[izu:'"
    more_izu_files = file:/var/local/content/articles_*.izu


## Rendering

Once Rig4 has a set of entries, it generates a site using both *renderers* and
*output generators*.

Renderers transform the entries into HTML.
At first Rig4 will have a single renderer for Izumi text syntax.
Eventually the goal is to have one pass-through renderer for HTML and maybe
later also support the Markdown syntax.

Output generators, as the name implies, generate the final site by using
HTML templates (header, body, footer) and filling it with the entries HTML.

The final HTML page is composed of *snippets*, for each the page header, the page
footer, a categories navigation bar and every single blog post are all individual
snippets. An HTML template indicate how to assemble them together.

Snippets are cached locally and only regenerated when they change.

The plan is to have 2 generators upfront:

- A JavaScript generator that creates a dynamic site where there's a single
  HTML page which content is loaded via AJAX.
- An Apache Server-Side Includes generator that creates a site where each
  snippet is embedded in the final page via #include statements. 

A site can have 3 kind of pages:
 
- An *Article Page* is a single page that contains a full article.
- A *Blog Page* is a paginated blog page that shows up to N recent articles
  with a footer to access the following pages.
- A *Blog Post Page* is a single page for a single blog post, accessed from
  a permalink in the parent blog page.


## Pipeline Overview

- **Sources** are parsed from the configuration.
- **Readers** read the data, which creates one or more documents.
- **Document headers** are parsed to extract Izumi tags.
- **Documents** are split into entries (articles are single entries, blogs are multiple entries.)
- An **output generator** is selected based on the configuration.
- The generator computes a graph of pages & entries to render.
- It then invokes the **page generators** which in turn invoke the HTML **renderers**.


## Golang Style

Source code style where it significantly differs from the recommended Go style:

- Interfaces are prefixed with a "I". In practice, I see a lot of value in explicitly
  knowing whether a type is an interface vs a struct.
    - Methods that receive an interface should not try to receive it as a pointer.
      This avoids the usual "pointer to interface" error message that so easily
      confuses developers.
    - Once it's clear an argument is an interface, it's equaly clear that the
      method will only access methods on the interface and not fields.
- Struct types have a clear comment indicating which interface they implement.
  I still consider it a failure that Go offers no way to provide that hint to the
  compiler -- which could then warn upfront whether the interface implementation is
  complete.
- Channels offers a convenient way to implement asynchronous generators. However
  that feature should not be abused. It's a lot harder to understand and debug
  asynchronous behavior, especially when it provides no clear benefit over
  returning a slice of objects.
- One of the issues I find too often is variable shadowing, for example when using
  variable affectation in a "if" statement inside a loop with same variable names.
  It is a common mistake to assume that "v,err := foo()" will reassign to a previously
  defined "err" variable: the spec says this is the case ONLY within the same scope.
  When used in different scopes, the creates a new shadow variable.
  So the rule is to never reuse the same variable name, like Java forces ones to do:
  any variable on the left of := cannot match one used in an immediate outer scope.

~~

