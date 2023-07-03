package com.alflabs.rig4k.dl

import com.alflabs.rig4k.common.Timing
import com.alflabs.utils.ILogger
import com.google.common.base.Charsets
import com.steadystate.css.parser.CSSOMParser
import com.steadystate.css.parser.SACParserCSS3
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import org.w3c.css.sac.InputSource
import org.w3c.dom.css.CSSStyleRule
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class HtmlToIzuParser @Inject constructor(
    private val logger: ILogger,
    private val timing: Timing,
) {
    fun transformToIzu(htmlContent: ByteArray): ByteArray {
        return timing.get("HtmlTransformer").time {
            var doc = Jsoup.parse(htmlContent.inputStream(),
                null /* charset (auto-discover or fallback to UTF-8) */,
                "" /* base uri */)
            expandStyles(doc)
//            doc = cleanup(doc)
//            removeEmptyElements(doc, ELEM_A)
//            removeEmptyElements(doc, ELEM_SPAN)

            val output = StringBuilder("")

            val body = doc.getElementsByTag("body").first()
            body?.let {
                parseBody(output, doc)
            }


            output.toString().toByteArray(Charsets.UTF_8)
        }
    }

    private fun expandStyles(doc: Document) {
        // Find the first <style type=text/css> element.
        val styleNodes = doc.getElementsByTag(ELEM_STYLE).filter { element ->
            element.attr(ATTR_TYPE) == "text/css"
        }
        assert(styleNodes.size == 1)    // TBD handle more than 1?
        val styleNode = styleNodes.first()
        if (styleNode == null) return
        val cssText : String = styleNode.data()

        // Parse the CSS styles.
        val parser = CSSOMParser(SACParserCSS3())
        try {
            val sheet = parser.parseStyleSheet(
                InputSource(cssText.reader()),
                null /* ownerNode*/,
                null /* href */
            )
            val cssRules = sheet?.cssRules
            if (cssRules == null || cssRules.length < 1) return

            // Create a map with all the CSS rules.
            // We only deal with simple class selectors (e.g. class="foo", match a CSS selector ".foo").
            // We could straight ignore any complex selector with a space, >, etc.
            val rulesMap = mutableMapOf<String, String>()
            for (i in 0 until cssRules.length) {
                val rule = cssRules.item(i)
                if (rule is CSSStyleRule) {
                    rulesMap[rule.selectorText] = rule.style.cssText
                }
            }

            // Update the document nodes in-place.
            val visitor = NodeVisitor { node, depth ->
                if (node is Element) {
                    if (node.tagName() == ELEM_STYLE) {
                        // Removes any <style type=text/css> node.
                        if (node.attr(ATTR_TYPE) == "text/css") {
                            node.remove()
                        }
                    } else {
                        // Iterate through element class names.
                        var modified = false
                        val nodeClasses = node.classNames()
                        val nodeClassesIterator = nodeClasses.iterator()
                        for (c in nodeClassesIterator) {
                            val cssStyle: String? = rulesMap[".$c"]
                            cssStyle?.let {
                                // Remove the class from the element class list.
                                nodeClassesIterator.remove()
                                modified = true
                                // And instead expand it in the style attribute.
                                var nodeStyle: String = node.attr(ATTR_STYLE)
                                if (nodeStyle.isNotEmpty()) nodeStyle += "; "
                                nodeStyle += cssStyle
                                node.attr(ATTR_STYLE, nodeStyle)
                            }
                        }
                        if (modified) {
                            node.classNames(nodeClasses)
                        }
                    }
                }
            }
            NodeTraversor.traverse(visitor, doc)


        } catch (e: IOException) {
            logger.d(TAG, "Failed to read CSS", e)
        }
    }

    /**
     * Use Jsoup's cleaner to remove all tags not in the "relaxed" group.
     * Things we keep for gdoc are HR and STYLE tags as well as all STYLE attributes.
     */
    private fun cleanup(doc: Document): Document {
        val relaxed = Safelist.relaxed()
        relaxed.preserveRelativeLinks(true)
        relaxed.addTags(ELEM_HR)
        relaxed.addTags(ELEM_STYLE)
        relaxed.addProtocols(
            ELEM_A,
            ATTR_HREF,
            "#"
        ) // allow internal anchors
        relaxed.addProtocols(
            ELEM_IMG,
            ATTR_SRC,
            "http",
            "https",
            "data"
        ) // also allow direct data
        relaxed.addAttributes(":all", ATTR_ID)
        relaxed.addAttributes(":all", ATTR_STYLE)
        val cleaner = Cleaner(relaxed)
        return cleaner.clean(doc)
    }

    /**
     * Remove all elements which have no child.
     * The GDoc export generates quite a number of useless ones.
     */
    private fun removeEmptyElements(root: Element, name: String) {
        for (element in root.getElementsByTag(name)) {
            if (element.childNodeSize() == 0) {
                element.remove()
            }
        }
    }

    private fun parseBody(output: StringBuilder, body: Element) {
        val visitor = NodeVisitor { node, depth ->
            val s = " ".repeat(depth)
            val classes = if (node is Element) {
                    node.classNames().joinToString()
                } else ""
            println("[$s$depth] : ${node::class.simpleName} : $classes : $node")
        }
        NodeTraversor.traverse(visitor, body)
    }

    companion object {
        private val TAG = HtmlToIzuParser::class.java.simpleName

        private const val ELEM_A = "a"
        private const val ELEM_DIV = "div"
        private const val ELEM_HR = "hr"
        private const val ELEM_IFRAME = "iframe"
        private const val ELEM_IMG = "img"
        private const val ELEM_LI = "li"
        private const val ELEM_P = "p"
        private const val ELEM_TD = "td"
        private const val ELEM_SPAN = "span"
        private const val ELEM_STYLE = "style"
        private const val ELEM_UL = "ul"

        private const val ATTR_ALT = "alt"
        private const val ATTR_CLASS = "class"
        private const val ATTR_HREF = "href"
        private const val ATTR_ID = "id"
        private const val ATTR_TYPE = "type"
        private const val ATTR_SRC = "src"
        private const val ATTR_STYLE = "style"
        private const val ATTR_TITLE = "title"
        private const val ATTR_WIDTH = "width"
        private const val ATTR_HEIGHT = "height"

        private const val QUERY_Q = "q"
        private const val QUERY_W = "w"
        private const val QEURY_H = "h"
        private const val QUERY_RIG4EMBED = "rig4embed"

        private const val FONT_CONSOLE = "Consolas"
        private const val CLASS_CONSOLE = "console"

        private val HTML_NBSP = Entities.getByName("nbsp")

    }
}
