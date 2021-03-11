/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.daemon.impl.quickfix.OrderEntryFix
import com.intellij.codeInsight.daemon.impl.quickfix.QuickFixActionRegistrarImpl
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference.ShorteningMode
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.runWhenSmart
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElement
import org.jetbrains.kotlin.psi.psiUtil.startOffset

object KotlinAddOrderEntryActionFactory : KotlinIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val simpleExpression = diagnostic.psiElement as? KtSimpleNameExpression ?: return emptyList()
        if (!ProjectRootsUtil.isInProjectSource(simpleExpression, includeScriptsOutsideSourceRoots = false)) return emptyList()

        val refElement = simpleExpression.getQualifiedElement()

        val reference = object : PsiReferenceBase<KtElement>(refElement) {
            override fun resolve() = null

            override fun getVariants() = PsiReference.EMPTY_ARRAY

            override fun getRangeInElement(): TextRange {
                val offset = simpleExpression.startOffset - refElement.startOffset
                return TextRange(offset, offset + simpleExpression.textLength)
            }

            override fun getCanonicalText() = refElement.text

            override fun bindToElement(element: PsiElement): PsiElement {
                val project = element.project
                project.runWhenSmart {
                    project.executeWriteCommand("") {
                        simpleExpression.mainReference.bindToElement(element, ShorteningMode.FORCED_SHORTENING)
                    }
                }
                return element
            }
        }

        @Suppress("UNCHECKED_CAST")
        return OrderEntryFix.registerFixes(QuickFixActionRegistrarImpl(null), reference) as List<IntentionAction>? ?: emptyList()
    }
}
