package org.jetbrains.dokka.analysis.java.doccomment

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.JavadocTag

/**
 * MUST override equals and hashcode
 */
@InternalDokkaApi
interface DocComment {
    fun hasTag(tag: JavadocTag): Boolean

    fun resolveTag(tag: JavadocTag): List<DocumentationContent>
}
