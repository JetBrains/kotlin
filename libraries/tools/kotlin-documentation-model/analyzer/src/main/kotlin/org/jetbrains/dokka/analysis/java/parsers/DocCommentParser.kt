/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.doccomment.DocComment
import org.jetbrains.dokka.model.doc.DocumentationNode

@InternalDokkaApi
internal interface DocCommentParser {
    fun canParse(docComment: DocComment): Boolean
    /**
     * @param useSite the element the analysis is performed from; used to create DRIs for
     * references that resolve to binary declarations. Defaults to [context] itself.
     */
    fun parse(docComment: DocComment, context: PsiNamedElement, sourceSet: DokkaSourceSet, useSite: PsiElement = context): DocumentationNode
}
