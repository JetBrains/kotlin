/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.utilities

import org.jetbrains.dokka.InternalDokkaApi
import java.net.URLEncoder


/**
 * Replaces symbols reserved in HTML with their respective entities.
 * Replaces & with &amp;, < with &lt; and > with &gt;
 */
@InternalDokkaApi
internal fun String.htmlEscape(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

@InternalDokkaApi
@Suppress("unused")
internal fun String.urlEncoded(): String = URLEncoder.encode(this, "UTF-8")

@InternalDokkaApi
@Suppress("unused")
internal fun String.formatToEndWithHtml(): String =
    if (endsWith(".html") || contains(Regex("\\.html#"))) this else "$this.html"
