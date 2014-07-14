package org.jetbrains.dokka

fun String.htmlEscape() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

