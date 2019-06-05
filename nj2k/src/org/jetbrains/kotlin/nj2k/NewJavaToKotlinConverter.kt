/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiStatement
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.psi.KtPsiFactory

class NewJavaToKotlinConverter(
    val project: Project,
    val settings: ConverterSettings,
    val oldConverterServices: JavaToKotlinConverterServices
) : JavaToKotlinConverter() {
    val converterServices = object : NewJavaToKotlinServices {
        override val oldServices = oldConverterServices
    }

    private val LOG = Logger.getInstance("#org.jetbrains.kotlin.j2k.JavaToKotlinConverter")

    override fun filesToKotlin(
        files: List<PsiJavaFile>,
        postProcessor: PostProcessor,
        progress: ProgressIndicator
    ): FilesResult {
        val phasesCount = postProcessor.phasesCount + 1
        val withProgressProcessor = NewJ2kWithProgressProcessor(progress, files, phasesCount)
        val (results, externalCodeProcessing, context) = withProgressProcessor.process {
            ApplicationManager.getApplication().runReadAction(Computable {
                elementsToKotlin(files, withProgressProcessor)
            })
        }

        val texts = results.mapIndexed { i, result ->
            try {
                val kotlinFile = ApplicationManager.getApplication().runReadAction(Computable {
                    KtPsiFactory(project).createFileWithLightClassSupport("dummy.kt", result!!.text, files[i])
                })

                runBlocking(EDT.ModalityStateElement(ModalityState.defaultModalityState())) {
                    withContext(EDT) {
                        CommandProcessor.getInstance().runUndoTransparentAction {
                            result!!.importsToAdd.forEach {
                                postProcessor.insertImport(kotlinFile, it)
                            }
                        }
                    }
                }
                withProgressProcessor.process {
                    AfterConversionPass(project, postProcessor).run(
                        kotlinFile,
                        context,
                        range = null,
                        onPhaseChanged = { phase, description ->
                            withProgressProcessor.updateState(i, phase + 1, description)
                        })
                }
                kotlinFile.text
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (t: Throwable) {
                LOG.error(t)
                result!!.text
            }
        }

        return FilesResult(texts, externalCodeProcessing)
    }

    override fun elementsToKotlin(inputElements: List<PsiElement>, processor: WithProgressProcessor): Result {
        @Suppress("NAME_SHADOWING")
        val processor = processor as NewJ2kWithProgressProcessor

        val phaseDescription = "Converting Java code to Kotlin code"
        val symbolProvider = JKSymbolProvider()
        symbolProvider.preBuildTree(inputElements)
        val importStorage = ImportStorage()
        val treeBuilder = JavaToJKTreeBuilder(symbolProvider, converterServices, importStorage)
        val asts = processor.process {
            inputElements.mapIndexed { i, element ->
                processor.updateState(i, 1, phaseDescription)
                element to treeBuilder.buildTree(element)
            }
        }

        val context = NewJ2kConverterContext(
            symbolProvider,
            this,
            { it.containingFile in inputElements },
            importStorage,
            JKElementInfoStorage()
        )
        processor.process {
            ConversionsRunner.doApply(asts.withIndex().mapNotNull { (i, ast) ->
                processor.updateState(i, 1, phaseDescription)
                ast.second
            }, context)
        }
        val results = processor.process {
            asts.mapIndexed { i, elementWithAst ->
                processor.updateState(i, 1, phaseDescription)
                val (element, ast) = elementWithAst
                if (ast == null) return@mapIndexed null
                val code = NewCodeBuilder(context).run { printCodeOut(ast) }
                val parseContext = when (element) {
                    is PsiStatement, is PsiExpression -> ParseContext.CODE_BLOCK
                    else -> ParseContext.TOP_LEVEL
                }
                ElementResult(
                    code,
                    importsToAdd = importStorage.getImports(),
                    parseContext = parseContext
                )
            }
        }

        return Result(results, null, context)
    }

    override fun elementsToKotlin(inputElements: List<PsiElement>): Result {
        return elementsToKotlin(inputElements, NewJ2kWithProgressProcessor.DEFAULT)
    }
}

class NewJ2kWithProgressProcessor(
    private val progress: ProgressIndicator?,
    private val files: List<PsiJavaFile>?,
    private val phasesCount: Int
) : WithProgressProcessor {
    companion object {
        val DEFAULT = NewJ2kWithProgressProcessor(null, null, 0)
    }

    override fun updateState(fileIndex: Int, phase: Int, description: String) {
        progress?.checkCanceled()
        progress?.fraction = phase / phasesCount.toDouble()
        progress?.text = "$description - phase $phase of $phasesCount"
        if (files != null && files.isNotEmpty()) {
            progress?.text2 = files[fileIndex].virtualFile.presentableUrl
        }
    }

    override fun <TInputItem, TOutputItem> processItems(
        fractionPortion: Double,
        inputItems: Iterable<TInputItem>,
        processItem: (TInputItem) -> TOutputItem
    ): List<TOutputItem> {
        throw AbstractMethodError("Should not be called for new J2K")
    }

    override fun <T> process(action: () -> T): T {
        // we use special process with EmptyProgressIndicator to avoid changing text in our progress by inheritors search inside etc
        var result: T? = null
        ProgressManager.getInstance().runProcess({ result = action() }, EmptyProgressIndicator())
        return result!!
    }

}