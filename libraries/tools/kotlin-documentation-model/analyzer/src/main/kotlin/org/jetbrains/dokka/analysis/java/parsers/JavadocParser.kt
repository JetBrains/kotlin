/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.doccomment.DocCommentFinder
import org.jetbrains.dokka.model.doc.DocumentationNode

internal interface JavaDocumentationParser {
    /**
     * @param useSite the element the analysis is performed from; used to create DRIs for
     * references that resolve to binary declarations. Defaults to [element] itself.
     */
    fun parseDocumentation(element: PsiNamedElement, sourceSet: DokkaSourceSet, useSite: PsiElement = element): DocumentationNode
}

@InternalDokkaApi
internal class JavadocParser(
    private val docCommentParsers: List<DocCommentParser>,
    private val docCommentFinder: DocCommentFinder
) : JavaDocumentationParser {

    override fun parseDocumentation(element: PsiNamedElement, sourceSet: DokkaSourceSet, useSite: PsiElement): DocumentationNode {
        val comment = docCommentFinder.findClosestToElement(element) ?: return DocumentationNode(emptyList())
        return docCommentParsers
            .first { it.canParse(comment) }
            .parse(comment, element, sourceSet, useSite)
    }
}
