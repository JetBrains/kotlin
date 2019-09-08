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
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.nj2k.ImportStorage
import org.jetbrains.kotlin.nj2k.asLabel
import org.jetbrains.kotlin.nj2k.inference.common.BoundTypeCalculatorImpl
import org.jetbrains.kotlin.nj2k.inference.common.ByInfoSuperFunctionsProvider
import org.jetbrains.kotlin.nj2k.inference.common.ConstraintsCollectorAggregator
import org.jetbrains.kotlin.nj2k.inference.common.InferenceFacade
import org.jetbrains.kotlin.nj2k.inference.common.collectors.CallExpressionConstraintCollector
import org.jetbrains.kotlin.nj2k.inference.common.collectors.CommonConstraintsCollector
import org.jetbrains.kotlin.nj2k.inference.common.collectors.FunctionConstraintsCollector
import org.jetbrains.kotlin.nj2k.inference.nullability.NullabilityBoundTypeEnhancer
import org.jetbrains.kotlin.nj2k.inference.nullability.NullabilityConstraintsCollector
import org.jetbrains.kotlin.nj2k.inference.nullability.NullabilityContextCollector
import org.jetbrains.kotlin.nj2k.inference.nullability.NullabilityStateUpdater
import org.jetbrains.kotlin.nj2k.postProcessing.postProcessing
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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

val nullabilityProcessing =
    postProcessing { file, rangeMarker, converterContext ->
        val resolutionFacade = file.getResolutionFacade()
        val inferenceFacade = InferenceFacade(
            NullabilityContextCollector(resolutionFacade, converterContext),
            ConstraintsCollectorAggregator(
                resolutionFacade,
                listOf(
                    CommonConstraintsCollector(),
                    CallExpressionConstraintCollector(),
                    FunctionConstraintsCollector(ByInfoSuperFunctionsProvider(resolutionFacade, converterContext)),
                    NullabilityConstraintsCollector()
                )
            ),
            BoundTypeCalculatorImpl(resolutionFacade, NullabilityBoundTypeEnhancer(resolutionFacade)),
            NullabilityStateUpdater()
        )
        val elements = if (rangeMarker != null) {
            file.elementsInRange(rangeMarker.range ?: return@postProcessing).filterIsInstance<KtElement>()
        } else listOf(file)

        inferenceFacade.runOn(elements)
    }

val clearUndefinedLabelsProcessing =
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

val shortenReferencesProcessing =
    postProcessing { file, rangeMarker, _ ->
        val filter = filter@{ element: PsiElement ->
            if (element !is KtQualifiedExpression) return@filter ShortenReferences.FilterResult.PROCESS
            val fqName = element.selectorExpression?.mainReference?.resolve()?.getKotlinFqName()
                ?: return@filter ShortenReferences.FilterResult.PROCESS
            if (ImportStorage.isImportNeeded(fqName)) ShortenReferences.FilterResult.PROCESS
            else ShortenReferences.FilterResult.SKIP
        }
        if (rangeMarker != null) {
            if (rangeMarker.isValid) {
                ShortenReferences.DEFAULT.process(file, rangeMarker.startOffset, rangeMarker.endOffset, filter)
            }
        } else {
            ShortenReferences.DEFAULT.process(file, filter)
        }
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
