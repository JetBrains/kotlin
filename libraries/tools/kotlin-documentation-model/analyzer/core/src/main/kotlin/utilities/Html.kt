package org.jetbrains.dokka.utilities

import org.jetbrains.dokka.*
import java.net.URLEncoder


/**
 * Replaces symbols reserved in HTML with their respective entities.
 * Replaces & with &amp;, < with &lt; and > with &gt;
 */
@InternalDokkaApi
fun String.htmlEscape(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

@InternalDokkaApi
fun String.urlEncoded(): String = URLEncoder.encode(this, "UTF-8")

@InternalDokkaApi
fun String.formatToEndWithHtml() =
    if (endsWith(".html") || contains(Regex("\\.html#"))) this else "$this.html"
