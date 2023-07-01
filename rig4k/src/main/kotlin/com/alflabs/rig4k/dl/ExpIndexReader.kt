package com.alflabs.rig4k.dl

import com.alflabs.rig4k.site.ArticleEntry
import com.alflabs.rig4k.site.BlogEntry
import com.alflabs.rig4k.site.Index
import com.alflabs.utils.ILogger
import com.google.common.base.Charsets
import com.google.common.base.Preconditions
import java.io.IOException
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpIndexReader @Inject constructor(
    private val logger: ILogger,
    private val gDocHelper: ExpGDocHelper,
) {
    companion object {
        private val TAG = ExpIndexReader::class.java.simpleName
        private val sArticleLineRe =
            Pattern.compile("^([a-z0-9_/-]+.html)\\s+([a-zA-Z0-9_-]+)\\s*")
        private val sBlogLineRe =
            Pattern.compile("^[bB]log\\s*([1-9]+)?\\s*(\\([^)]*\\))?\\s+([a-zA-Z0-9_-]+)\\s*")
    }

    @Throws(IOException::class)
    fun readIndex(indexId: String): Index {
        logger.d(TAG, "Processing document: index $indexId")
        val entity = gDocHelper.getGDocSync(indexId, "text/plain")
        Preconditions.checkNotNull(entity)

        val content = String(entity!!.getContent()!!, Charsets.UTF_8)
        val articleEntries = mutableListOf<ArticleEntry>()
        val blogEntries = mutableListOf<BlogEntry>()

        for (line in content
            .split("\n".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
        ) {
            val _line = line.trim { it <= ' ' }
            var matcher = sArticleLineRe.matcher(_line)
            if (matcher.find()) {
                articleEntries.add(ArticleEntry(matcher.group(2), matcher.group(1)))
                continue
            }
            matcher = sBlogLineRe.matcher(_line)
            if (matcher.find()) {
                val siteNumber = matcher.group(1)?.toIntOrNull() ?: 0
                blogEntries.add(BlogEntry(matcher.group(3), siteNumber))
            }
        }
        return Index(articleEntries, blogEntries)
    }
}
