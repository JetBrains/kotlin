/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.codeInsight.javadoc.JavaDocExternalFilter
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import java.util.function.Consumer

// FIX ME WHEN BUNCH 201 REMOVED
class KotlinDocumentationProvider : KotlinDocumentationProviderCompatBase() {

    override fun collectDocComments(file: PsiFile, sink: Consumer<in PsiDocCommentBase>) {
        if (file !is KtFile) return

        PsiTreeUtil.processElements(file) {
            val comment = (it as? KtDeclaration)?.docComment
            if (comment != null) sink.accept(comment)
            true
        }
    }

    override fun generateRenderedDoc(element: PsiElement): String? {
        val docComment = (element as? KtDeclaration)?.docComment ?: return null

        val result = StringBuilder().also {
            it.renderKDoc(docComment.getDefaultSection(), docComment.getAllSections())
        }

        return JavaDocExternalFilter.filterInternalDocInfo(result.toString())
    }
}
