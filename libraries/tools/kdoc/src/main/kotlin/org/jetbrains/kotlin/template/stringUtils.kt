package org.jetbrains.kotlin.template

fun String.escapeHtml() =
    this
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
