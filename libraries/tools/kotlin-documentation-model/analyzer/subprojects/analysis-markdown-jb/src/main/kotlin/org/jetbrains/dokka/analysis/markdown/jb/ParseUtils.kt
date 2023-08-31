/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb

import org.intellij.markdown.lexer.Compat
import org.intellij.markdown.lexer.Compat.forEachCodePoint
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.Text
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.Entities

@InternalDokkaApi
public fun String.parseHtmlEncodedWithNormalisedSpaces(
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
        } else if (Compat.codePointToString(c).let { it != Entities.escape(it) }) {
            accum.toString().takeIf { it.isNotBlank() }?.let { tags.add(Text(it)) }
            accum.delete(0, accum.length)

            accum.appendCodePoint(c)
            tags.add(Text(accum.toString(), params = DocTag.contentTypeParam("html")))
            accum.delete(0, accum.length)
        } else if (!StringUtil.isInvisibleChar(c)) {
            accum.appendCodePoint(c)
            lastWasWhite = false
        }
    }
    accum.toString().takeIf { it.isNotBlank() }?.let { tags.add(Text(it)) }
    return tags
}
