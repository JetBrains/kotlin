/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter.formatHtml

fun formatHtml(html: String): String {
    var message = html
    fun wrapTagWithNewLines(tagName: String, isSubTag: Boolean) {
        val openTagPart = "<$tagName"
        val indent = if (isSubTag) "  " else ""
        message = message.replace(openTagPart, "\n$indent$openTagPart")

        val tagPart = if (isSubTag) "/$tagName>" else "$tagName>"
        message = message.replace(tagPart, "$tagPart\n")
    }

    for (tag in listOf("html", "ul", "table", "tr")) {
        wrapTagWithNewLines(tag, isSubTag = false)
    }
    for (tag in listOf("li", "td")) {
        wrapTagWithNewLines(tag, isSubTag = true)
    }
    message = message.replace("<br/>", "<br/>\n").replace("&nbsp;", " ")
    message = message.replace("&lt;", "<`").replace("&gt;", ">")
    message = message.lines().map { it.trimEnd() }.filterNot { it.isEmpty () }.joinToString("\n")
    return message
}
