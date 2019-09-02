/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing.processings

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.nj2k.asLabel
import org.jetbrains.kotlin.nj2k.postProcessing.postProcessing
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange

val formatCodeProcessing =
    postProcessing { file, rangeMarker, _ ->
        file.commitAndUnblockDocument()
        val codeStyleManager = CodeStyleManager.getInstance(file.project)
        if (rangeMarker != null) {
            if (rangeMarker.isValid) {
                codeStyleManager.reformatRange(file, rangeMarker.startOffset, rangeMarker.endOffset)
            }
        } else {
            codeStyleManager.reformat(file)
        }
    }

val clearUnknownLabelsProcessing =
    postProcessing { file, _, _ ->
        file.clearUndefinedLabels()
    }

private fun KtFile.clearUndefinedLabels() {
    val comments = mutableListOf<PsiComment>()
    accept(object : PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        override fun visitComment(comment: PsiComment) {
            if (comment.text.asLabel() != null) {
                comments += comment
            }
        }
    })
    comments.forEach { it.delete() }
}


val optimizeImportsProcessing =
    postProcessing { file, rangeMarker, _ ->
        val elements = when {
            rangeMarker != null && rangeMarker.isValid -> file.elementsInRange(rangeMarker.range!!)
            rangeMarker != null && !rangeMarker.isValid -> emptyList()
            else -> file.children.asList()
        }
        val needFormat = elements.any { element ->
            element is KtElement
                    && element !is KtImportDirective
                    && element !is KtImportList
                    && element !is KtPackageDirective
        }
        if (needFormat) {
            OptimizeImportsProcessor(file.project, file).run()
        }
    }
