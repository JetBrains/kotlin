/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.doccomment.DocCommentFinder
import org.jetbrains.dokka.model.doc.DocumentationNode

internal fun interface JavaDocumentationParser {
    fun parseDocumentation(element: PsiNamedElement): DocumentationNode
}

@InternalDokkaApi
public class JavadocParser(
    private val docCommentParsers: List<DocCommentParser>,
    private val docCommentFinder: DocCommentFinder
) : JavaDocumentationParser {

    override fun parseDocumentation(element: PsiNamedElement): DocumentationNode {
        val comment = docCommentFinder.findClosestToElement(element) ?: return DocumentationNode(emptyList())
        return docCommentParsers
            .first { it.canParse(comment) }
            .parse(comment, element)
    }
}
