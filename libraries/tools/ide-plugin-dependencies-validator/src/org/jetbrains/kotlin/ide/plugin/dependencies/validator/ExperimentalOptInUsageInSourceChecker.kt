/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.plugin.dependencies.validator

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.nio.file.Path
import kotlin.io.path.exists

object ExperimentalOptInUsageInSourceChecker {
    fun checkExperimentalOptInUsage(srcRoots: List<Path>): List<ExperimentalAnnotationUsage> {
        return buildList {
            srcRoots
                .filter { it.exists() }
                .forEach { srcRoot ->
                    forEachKtFileInDirectory(srcRoot) { ktFile, path ->
                        addAll(checkExperimentalOptInUsage(ktFile, path))
                    }
                }
        }
    }

    private fun checkExperimentalOptInUsage(ktFile: KtFile, file: Path): List<ExperimentalAnnotationUsage> {
        return ktFile
            .collectDescendantsOfType<KtAnnotationEntry>()
            .flatMap { annotationEntry ->
                val annotationShortName = annotationEntry.shortName?.asString() ?: return@flatMap emptyList()
                val experimentalAnnotations = when (annotationShortName) {
                    OPT_IN_ANNOTATION -> {
                        annotationEntry.valueArguments.mapNotNull mapArguments@{ argument ->
                            if (argument !is KtValueArgument) return@mapArguments null
                            val expression = argument.getArgumentExpression() as? KtClassLiteralExpression ?: return@mapArguments null
                            val classReference = expression.receiverExpression as? KtNameReferenceExpression ?: return@mapArguments null
                            classReference.getReferencedName().takeIf { it in ExperimentalAnnotations.experimentalAnnotationShortNames }
                        }
                    }
                    in ExperimentalAnnotations.experimentalAnnotationShortNames -> {
                        listOf(annotationShortName)
                    }
                    else -> return@flatMap emptyList()
                }
                if (experimentalAnnotations.isEmpty()) return@flatMap emptyList()

                /* offsetToLineNumber's indexing starts from 0*/
                val lineNumber = StringUtil.offsetToLineNumber(ktFile.text, annotationEntry.startOffset) + 1

                experimentalAnnotations.map { annotation ->
                    ExperimentalAnnotationUsage(file, lineNumber, annotation)
                }
            }
    }


    private const val OPT_IN_ANNOTATION = "OptIn"
}