package org.jetbrains.kotlin.codegen

import java.net.URL
import java.net.URLClassLoader

fun ClassLoader?.extractUrls(): List<URL> {
    return (this as? URLClassLoader)?.let {
        it.urLs.toList() + it.parent.extractUrls()
    } ?: emptyList()
}
