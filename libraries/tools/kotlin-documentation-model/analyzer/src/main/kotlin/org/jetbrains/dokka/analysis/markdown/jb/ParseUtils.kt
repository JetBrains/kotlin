/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb

import org.intellij.markdown.lexer.Compat
import org.intellij.markdown.lexer.Compat.forEachCodePoint
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.Text
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.TextNode

@InternalDokkaApi
internal fun String.parseHtmlEncodedWithNormalisedSpaces(
    renderWhiteCharactersAsSpaces: Boolean
): List<DocTag> {
    val accum = StringBuilder()
    val tags = mutableListOf<DocTag>()
    var lastWasWhite = false

    forEachCodePoint { c ->
        when {
            StringUtil.isWhitespace(c) -> {
                if (renderWhiteCharactersAsSpaces) {
                    if (!lastWasWhite) {
                        accum.append(' ')
                        lastWasWhite = true
                    }
                } else {
                    accum.appendCodePoint(c)
                }
            }
            // TextNode(...).outerHtml() produces HTML created from the passed text, so it will perform HTML body escaping.
            // We do only check here if the symbol needs HTML escaping to split text into separate tags.
            Compat.codePointToString(c).let { it != TextNode(it).outerHtml() } -> {
                accum.toString().takeIf { it.isNotBlank() }?.let { tags.add(Text(it)) }
                accum.delete(0, accum.length)

                accum.appendCodePoint(c)
                tags.add(Text(accum.toString(), params = DocTag.contentTypeParam("html")))
                accum.delete(0, accum.length)
            }
            !StringUtil.isInvisibleChar(c) -> {
                accum.appendCodePoint(c)
                lastWasWhite = false
            }
        }
    }
    accum.toString().takeIf { it.isNotBlank() }?.let { tags.add(Text(it)) }
    return tags
}
