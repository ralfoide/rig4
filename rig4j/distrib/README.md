## Running Rig4j:

Check `$JAVA_HOME` is defined. Any JRE or JDK 1.8 will do.

Git pull, build & execute (expect Bash on Linux or Cygwin):

    rig4j/distrib/_pull.sh
    rig4j/distrib/rig4.sh

The `rig4.sh` wrapper will automatically rebuild if git pull got a new version.

First time need to deal with OAuth2:
* Run the app using `rig4.sh`.
* Follow the link from "Enter Client ID and Secret from"...
* Select the rig4 project.
* If the console shows the IAM (admin) screen, use 3-dot menu > API > Credentials.
* Select Java Rig4 Json and download JSON.
* Store it in the path indicated in the console error.
* Note: Use cygwin to create the `.rig42` folder, can't create it using Win Explorer.
* Run `rig4.sh` again and follow the web browser OAuth2 verification screen.
* The console app will auto-update.

## IJ Setup for Developing:

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

Config for testing: "C:/Users/$USER/.rig42rc"

    exp-doc-id = ...doc-id...
    exp-dest-dir = c:/Temp/trains
    exp-ga-uid = ...ua-id...
    exp-site-banner = http://ralf.alfray.com/trains/header.jpg
    exp-site-title = Test Train Page
    exp-site-base-url = http://localhost/

