package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.java

import org.jetbrains.dokka.analysis.java.doccomment.DocumentationContent
import org.jetbrains.dokka.analysis.java.JavadocTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag

internal data class DescriptorDocumentationContent(
    val resolveDocContext: ResolveDocContext,
    val element: KDocTag,
    override val tag: JavadocTag,
) : DocumentationContent {
    override fun resolveSiblings(): List<DocumentationContent> {
        return listOf(this)
    }
}
