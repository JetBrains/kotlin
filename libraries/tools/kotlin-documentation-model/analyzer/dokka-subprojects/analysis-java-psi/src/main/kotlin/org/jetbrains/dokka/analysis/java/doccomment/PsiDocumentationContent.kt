/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.doccomment

import com.intellij.psi.PsiElement
import com.intellij.psi.javadoc.PsiDocTag
import org.jetbrains.dokka.analysis.java.JavadocTag
import org.jetbrains.dokka.analysis.java.util.contentElementsWithSiblingIfNeeded

internal data class PsiDocumentationContent(
    val psiElement: PsiElement,
    override val tag: JavadocTag
) : DocumentationContent {

    override fun resolveSiblings(): List<DocumentationContent> {
        return if (psiElement is PsiDocTag) {
            psiElement.contentElementsWithSiblingIfNeeded()
                .map { content -> PsiDocumentationContent(content, tag) }
        } else {
            listOf(this)
        }
    }

}
