package com.alflabs.rig4k.dl

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GDocReaderOptions @Inject constructor(): OptionGroup("GDocReader Options") {
    val gdocRootDir by option(
        help = "Directory where Google Drive API stores credentials files.")
        .default("~/.rig42")

    val gdocPathClientSecretJson by option(
        help = "Path to load client_secret.json from Google Drive API.")
        .default("\$GDOC_ROOT_DIR/client_secret.json")

    val gdocPathDataStoreDir by option(
        help = "Directory where the Google Drive API stores local credentials.")
        .default("\$GDOC_ROOT_DIR/gdoc_store")
}
