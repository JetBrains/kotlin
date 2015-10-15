/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
