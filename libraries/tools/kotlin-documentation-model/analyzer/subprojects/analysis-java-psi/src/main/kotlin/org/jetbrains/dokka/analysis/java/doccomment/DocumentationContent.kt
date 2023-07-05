package org.jetbrains.dokka.analysis.java.doccomment

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.JavadocTag

@InternalDokkaApi
interface DocumentationContent {
    val tag: JavadocTag

    fun resolveSiblings(): List<DocumentationContent>
}
