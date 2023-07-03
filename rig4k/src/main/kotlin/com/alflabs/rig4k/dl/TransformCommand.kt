package com.alflabs.rig4k.dl

import com.alflabs.rig4k.common.Timing
import com.alflabs.rig4k.site.Site
import com.alflabs.rig4k.site.SiteOptions
import com.alflabs.utils.ILogger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransformCommand @Inject constructor(
    private val logger: ILogger,
    private val timing: Timing,
    private val gDocReader: GDocReader,
    private val siteOptions: SiteOptions,
    private val indexReader: IndexReader,
    private val entityFactory: EntityFactory,
    private val htmlToIzuParser: HtmlToIzuParser,
    gDocReaderOptions: GDocReaderOptions,
): CliktCommand(name = "transform", help = "Transform GDocs HTML into Izu") {
    companion object {
        private val TAG = TransformCommand::class.java.simpleName
    }

    @Suppress("unused")
    private val _gDocReaderOptions by gDocReaderOptions

    override fun run() {
        gDocReader.init()
        timing.get("Total").time {
            // Fetch the index.
            val site = Site(entityFactory.index(siteOptions.indexGdocId))
            site.index.preloadFromCache()
            indexReader.readIndex(site)
            // Transform content for articles.
            site.articleEntries.forEach {
                it.entity.preloadFromCache()
                logger.d(TAG, "Transom article entity: ${it.entity.fileId} -- ${it.entity.metadata.title} -- ${it.destName}")
                if (it.destName.contains("style")) {
                     transformArticle(it.entity)
                }
            }
        }
        timing.printToLog()
    }

    private fun transformArticle(entity: ArticleEntity) {
        htmlToIzuParser.transformToIzu(entity.getContent())
    }
}
