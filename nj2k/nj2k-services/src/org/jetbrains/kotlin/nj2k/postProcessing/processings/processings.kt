/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing.processings

import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.AnalysisScope
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.NullabilityAnalysisFacade
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.nullabilityByUndefinedNullabilityComment
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.prepareTypeElementByMakingAllTypesNullableConsideringNullabilityComment
import org.jetbrains.kotlin.nj2k.postProcessing.postProcessing

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
        NullabilityAnalysisFacade(
            converterContext,
            getTypeElementNullability = ::nullabilityByUndefinedNullabilityComment,
            prepareTypeElement = ::prepareTypeElementByMakingAllTypesNullableConsideringNullabilityComment,
            debugPrint = false
        ).fixNullability(AnalysisScope(file, rangeMarker))
    }

val shortenReferencesProcessing =
    postProcessing { file, rangeMarker, _ ->
        if (rangeMarker != null) {
            ShortenReferences.DEFAULT.process(file, rangeMarker.startOffset, rangeMarker.endOffset)
        } else {
            ShortenReferences.DEFAULT.process(file)
        }
    }

