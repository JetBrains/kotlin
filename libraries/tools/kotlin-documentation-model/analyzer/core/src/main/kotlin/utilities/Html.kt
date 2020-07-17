package org.jetbrains.dokka.utilities

import java.net.URLEncoder


/**
 * Replaces symbols reserved in HTML with their respective entities.
 * Replaces & with &amp;, < with &lt; and > with &gt;
 */
fun String.htmlEscape(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

fun String.urlEncoded(): String = URLEncoder.encode(this, "UTF-8")

fun String.formatToEndWithHtml() =
    if (endsWith(".html") || contains(Regex("\\.html#"))) this else "$this.html"