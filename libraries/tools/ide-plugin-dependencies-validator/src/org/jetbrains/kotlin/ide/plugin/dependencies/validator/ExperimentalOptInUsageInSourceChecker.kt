/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.plugin.dependencies.validator

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.impl.PsiFileFactoryImpl
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.streams.asSequence
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import kotlin.io.path.name
import kotlin.io.path.readText

object ExperimentalOptInUsageInSourceChecker {
    fun checkExperimentalOptInUsage(srcRoots: List<Path>): List<ExperimentalAnnotationUsage> {
        val project = createProjectForParsing()
        try {
            return srcRoots.filter { it.exists() }
                .flatMap { srcRoot ->
                    Files.walk(srcRoot)
                        .asSequence()
                        .filter { it.extension == "kt" }
                        .flatMap { file ->
                            val ktFile = file.parseAsKtFile(project)
                            checkExperimentalOptInUsage(ktFile, file)
                        }
                }
        } finally {
            Disposer.dispose(project)
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

    private fun Path.parseAsKtFile(project: Project): KtFile {
        return PsiFileFactoryImpl(project).createFileFromText(name, KotlinLanguage.INSTANCE, readText()) as KtFile
    }

    private fun createProjectForParsing(): Project {
        return KotlinCoreEnvironment.createForProduction(
            Disposer.newDisposable("Disposable for project of ${ExperimentalOptInUsageInSourceChecker::class.simpleName}"),
            CompilerConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).project
    }

    private const val OPT_IN_ANNOTATION = "OptIn"
}