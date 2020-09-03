/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.fir.highlighter.visitors.FirAfterResolveHighlightingVisitor
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.highlighter.AbstractKotlinPsiChecker
import org.jetbrains.kotlin.idea.highlighter.Diagnostic2Annotation
import org.jetbrains.kotlin.idea.highlighter.IdeErrorMessages
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

class KotlinFirPsiChecker : AbstractKotlinPsiChecker() {
    override fun shouldHighlight(file: KtFile): Boolean {
        return true // todo
    }

    override fun annotateElement(element: PsiElement, containingFile: KtFile, holder: AnnotationHolder) {
        if (element !is KtElement) return
        if (ApplicationManager.getApplication().isDispatchThread) {
            throw ProcessCanceledException()
        }
        analyze(element) {
            FirAfterResolveHighlightingVisitor
                .createListOfVisitors(this, holder)
                .forEach(element::accept)
        }
    }
}