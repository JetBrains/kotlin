package org.jetbrains.dokka.base.translators

import org.intellij.markdown.lexer.Compat.codePointToString
import org.intellij.markdown.lexer.Compat.forEachCodePoint
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.DocTag.Companion.contentTypeParam
import org.jetbrains.dokka.model.doc.Text
import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.Entities

internal fun String.parseHtmlEncodedWithNormalisedSpaces(
    renderWhiteCharactersAsSpaces: Boolean
): List<DocTag> {
    val accum = StringBuilder()
    val tags = mutableListOf<DocTag>()
    var lastWasWhite = false

    forEachCodePoint { c ->
        if (renderWhiteCharactersAsSpaces && StringUtil.isWhitespace(c)) {
            if (!lastWasWhite) {
                accum.append(' ')
                lastWasWhite = true
            }
        } else if (codePointToString(c).let { it != Entities.escape(it) }) {
            accum.toString().takeIf { it.isNotBlank() }?.let { tags.add(Text(it)) }
            accum.delete(0, accum.length)

            accum.appendCodePoint(c)
            tags.add(Text(accum.toString(), params = contentTypeParam("html")))
            accum.delete(0, accum.length)
        } else if (!StringUtil.isInvisibleChar(c)) {
            accum.appendCodePoint(c)
            lastWasWhite = false
        }
    }
    accum.toString().takeIf { it.isNotBlank() }?.let { tags.add(Text(it)) }
    return tags
}

/**
 * Parses string into [Text] doc tags that can have either value of the string or html-encoded value with content-type=html parameter.
 * Content type is added when dealing with html entries like `&nbsp;`
 */
internal fun String.parseWithNormalisedSpaces(
    renderWhiteCharactersAsSpaces: Boolean
): List<DocTag> =
    //parsing it using jsoup is required to get codePoints, otherwise they are interpreted separately, as chars
    //But we dont need to do it for java as it is already parsed with jsoup
    Jsoup.parseBodyFragment(this).body().wholeText().parseHtmlEncodedWithNormalisedSpaces(renderWhiteCharactersAsSpaces)