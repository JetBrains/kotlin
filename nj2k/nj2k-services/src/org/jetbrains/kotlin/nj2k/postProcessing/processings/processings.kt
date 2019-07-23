/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing.processings

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.AnalysisScope
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.NullabilityAnalysisFacade
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.nullabilityByUndefinedNullabilityComment
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.prepareTypeElementByMakingAllTypesNullableConsideringNullabilityComment
import org.jetbrains.kotlin.nj2k.postProcessing.postProcessing
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange

val formatCodeProcessing =
    postProcessing { file, rangeMarker, _ ->
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
        NullabilityAnalysisFacade(
            converterContext,
            getTypeElementNullability = { nullabilityByUndefinedNullabilityComment(it, converterContext) },
            prepareTypeElement = { prepareTypeElementByMakingAllTypesNullableConsideringNullabilityComment(it, converterContext) },
            debugPrint = false
        ).fixNullability(AnalysisScope(file, rangeMarker))
    }

val shortenReferencesProcessing =
    postProcessing { file, rangeMarker, _ ->
        if (rangeMarker != null) {
            if (rangeMarker.isValid) {
                ShortenReferences.DEFAULT.process(file, rangeMarker.startOffset, rangeMarker.endOffset)
            }
        } else {
            ShortenReferences.DEFAULT.process(file)
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
