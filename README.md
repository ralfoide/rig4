# Rig4j

## Description

`Rig4j` is an _experimental_ reimplementation of my site generator.

The project is highly experimental and may change at any time.
**Do not use for production.**

The goal of `Rig4` is to produce a static version of an HTML web site based on some source files.
This derives from an older site generator named
[Izumi](https://www.alfray.com/labs/archived/Izumi/)
that I wrote years ago. Izumi was using its own text-based syntax influenced by the early
WikiWiki syntax. Each Izumi text file generated a single HTML file.

`Rig4j` reuses that concept. However in this early experiment, the source files can be
read locally (like `Izumi`) or pulled from a Google Docs repository. Each Google Doc is
downloaded as an HTML rendition, which is then "cleaned up" as used as-is. Not every
object included in the Google Docs can be rendered -- only images and drawings.

Eventually, `Rig4j` was never totally finished -- it's still lacking a full `Izumi` 
backward-compatible mode -- as I then explored a few other variations.
`Rig4r` is an attempt at rebooting the project in Rust, which I quickly abandoned.
`Rig4k` is a redesign written in Kotlin, which I never quite fully finished.


## Usage

### Running Rig4j:

Check `$JAVA_HOME` is defined. Any JRE or JDK 1.8 will do.

Git pull, build & execute (expect Bash on Linux, Cygwin, or MSYS):

    rig4j/distrib/_pull.sh
    rig4j/distrib/rig4.sh

The [rig4.sh](distrib/rig4.sh) wrapper will automatically rebuild if git pull got a new version.

First time need to deal with OAuth2 to access Google Docs files:
* Run the app using `rig4.sh`.
* Follow the link from "Enter Client ID and Secret from"...
* Select the rig4 project.
* If the console shows the IAM (admin) screen, use 3-dot menu > API > Credentials.
* Select Java Rig4 Json and download JSON.
* Store it in the path indicated in the console error.
* Note: Use Cygwin or MSYS to create the `.rig42` folder, can't create it using Win Explorer.
* Run `rig4.sh` again and follow the web browser OAuth2 verification screen.
* The console app will auto-update.


### Configuration

By default, a configuration file is expected to be located
at  `C:\Users\%USER%\.rig42rc` or `~/.rig42rc`:

    exp-doc-id = ...doc-id...
    exp-dest-dir = c:/Temp/GeneratedSiteFolder
    exp-ga-uid = ...ua-id...
    exp-site-banner = http://web.site.domain/path/header.jpg
    exp-site-title = My Web Site Name
    exp-site-base-url = http://localhost/

The Google Doc ID must point to an "index" file.
This is a Google Doc file with a list of "page.html ID" lines.
Example:

    index.html          google-doc-id1
    article.html        google-doc-id2
    Blog Name           google-doc-id3

A Google Doc ID is a string such as "1dONgR1cS5073jHYlxyktt7s-TzJg5EtEzMkf_uozPNs"
which you can find by looking at the URL of any document in Google Drive.
Simply enter it without quotes in the index and the exp-doc-id.

The generator stores data in a cache directory located
at `C:\Users\%USER%\.rig42` or `~/.rig42`.

You can configure all these paths using command-line arguments.
Use `--help` for details.


### IJ Setup for Developing:

Settings > Build > Gradle > Runner: Delegate IDE actions to gradle.

Application:
* Name: Entry Point
* Single Instance Only
* Main class: com.alflabs.rig4.EntryPoint
* Module: rig4j
* Working Dir: .../rig4j
* JRE: Default (1.8 SDK)

Gradle:
* Name: All Tests
* Single Instance Only
* Project: rig4j


## License

Unless otherwise noted, all sources in this project are under the GPL 3.0 License.

See [LICENSE-gpl-3.0.txt](LICENSE-gpl-3.0.txt)

> Project: Rig4   
> Copyright (C) 2015 alf.labs gmail com,
>
>  This program is free software: you can redistribute it and/or modify
>  it under the terms of the GNU General Public License as published by
>  the Free Software Foundation, either version 3 of the License, or
>  (at your option) any later version.
>
>  This program is distributed in the hope that it will be useful,
>  but WITHOUT ANY WARRANTY; without even the implied warranty of
>  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
>  GNU General Public License for more details.
>
>  You should have received a copy of the GNU General Public License
>  along with this program.  If not, see <http://www.gnu.org/licenses/>.

~~
