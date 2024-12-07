/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp

import org.apache.commons.text.StringEscapeUtils
import java.io.PrintWriter

const val NON_BREAKING_HYPHEN = "&#8209;"

fun String?.escapeHtml(): String {
    if (this == null) return "NULL"
    return StringEscapeUtils.escapeHtml4(this)
}

fun Any?.toHtmlString(): String {
    if (this == null) return "NULL"
    return StringEscapeUtils.escapeHtml4(toString()).replace("\n", "<br>")
}

fun PrintWriter.tag(tagName: String) {
    print("<$tagName/>")
}

inline fun PrintWriter.tag(tagName: String, body: () -> Unit) {
    print("<$tagName>")
    body()
    println("</$tagName>")
}

fun PrintWriter.tag(tagName: String, content: String) {
    println("<$tagName>$content</$tagName>")
}

inline fun PrintWriter.table(body: () -> Unit) {
    tag("table", body)
}

fun PrintWriter.tableHeader(vararg headers: String) {
    tag("tr") {
        for (header in headers) {
            tag("th", header)
        }
    }
}

fun PrintWriter.tableData(vararg data: String) {
    tag("tr") {
        for (d in data) {
            tag("td", d)
        }
    }
}

fun PrintWriter.tableDataWithClass(tdClass: String, vararg data: String) {
    println("<tr>")
    for (d in data) {
        println("<td class=\"$tdClass\">$d</td>")
    }
    println("</tr>")
}

fun Any.tag(tagName: String) =
        "<$tagName>$this</$tagName>"