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
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.*
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.printing.JKCodeBuilder
import org.jetbrains.kotlin.nj2k.types.JKTypeFactory
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath

class NewJavaToKotlinConverter(
    val project: Project,
    private val targetModule: Module?,
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
        progress.isIndeterminate = false
        val phasesCount = postProcessor.phasesCount + 1
        val withProgressProcessor = NewJ2kWithProgressProcessor(progress, files, phasesCount)
        return withProgressProcessor.process {
            val (results, externalCodeProcessing, context) =
                ApplicationManager.getApplication().runReadAction(Computable {
                    elementsToKotlin(files, withProgressProcessor)
                })

            val texts = results.mapIndexed { i, result ->
                try {
                    val kotlinFile = ApplicationManager.getApplication().runReadAction(Computable {
                        KtPsiFactory(project).createFileWithLightClassSupport("dummy.kt", result!!.text, files[i])
                    })

                    ApplicationManager.getApplication().invokeAndWait {
                        CommandProcessor.getInstance().runUndoTransparentAction {
                            runWriteAction {
                                kotlinFile.addImports(result!!.importsToAdd)
                            }
                        }
                    }
                    AfterConversionPass(project, postProcessor).run(
                        kotlinFile,
                        context,
                        range = null,
                        onPhaseChanged = { phase, description ->
                            withProgressProcessor.updateState(i, phase + 1, description)
                        }
                    )
                    kotlinFile.text
                } catch (e: ProcessCanceledException) {
                    throw e
                } catch (t: Throwable) {
                    LOG.error(t)
                    result!!.text
                }
            }
            FilesResult(texts, externalCodeProcessing)
        }
    }

    private fun KtFile.addImports(imports: Collection<FqName>) {
        val factory = KtPsiFactory(this)
        var importList = importList
        for (import in imports) {
            val importDirective = factory.createImportDirective(ImportPath(import, isAllUnder = false))
            if (importList == null) {
                importList = addImportList(importDirective.parent as KtImportList)
            } else {
                importList.add(importDirective)
            }
        }
    }

    private fun KtFile.addImportList(importList: KtImportList): KtImportList {
        if (packageDirective != null) {
            return addAfter(importList, packageDirective) as KtImportList
        }

        val firstDeclaration = findChildByClass(KtDeclaration::class.java)
        if (firstDeclaration != null) {
            return addBefore(importList, firstDeclaration) as KtImportList
        }

        return add(importList) as KtImportList
    }


    override fun elementsToKotlin(inputElements: List<PsiElement>, processor: WithProgressProcessor): Result {
        val phaseDescription = "Converting Java code to Kotlin code"
        val contextElement = inputElements.firstOrNull() ?: return Result(emptyList(), null, null)
        val symbolProvider = JKSymbolProvider(project, targetModule, contextElement)
        val typeFactory = JKTypeFactory(symbolProvider)
        symbolProvider.typeFactory = typeFactory
        symbolProvider.preBuildTree(inputElements)

        val languageVersion = when {
            contextElement.isPhysical -> contextElement.languageVersionSettings
            else -> LanguageVersionSettingsImpl.DEFAULT
        }

        val importStorage = JKImportStorage(languageVersion)
        val treeBuilder = JavaToJKTreeBuilder(symbolProvider, typeFactory, converterServices, importStorage)

        // we want to leave all imports as is in the case when user is converting only imports
        val saveImports = inputElements.all { element ->
            element is PsiComment || element is PsiWhiteSpace
                    || element is PsiImportStatementBase || element is PsiImportList
                    || element is PsiPackageStatement
        }

        val asts = inputElements.mapIndexed { i, element ->
            processor.updateState(i, 1, phaseDescription)
            element to treeBuilder.buildTree(element, saveImports)
        }

        val context = NewJ2kConverterContext(
            symbolProvider,
            typeFactory,
            this,
            { it.containingFile in inputElements },
            importStorage,
            JKElementInfoStorage()
        )
        ConversionsRunner.doApply(asts.withIndex().mapNotNull { (i, ast) ->
            processor.updateState(i, 1, phaseDescription)
            ast.second
        }, context)

        val results = asts.mapIndexed { i, elementWithAst ->
            processor.updateState(i, 1, phaseDescription)
            val (element, ast) = elementWithAst
            if (ast == null) return@mapIndexed null
            val code = JKCodeBuilder(context).run { printCodeOut(ast) }
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
        var result: T? = null
        ProgressManager.getInstance().runProcess({ result = action() }, progress)
        return result!!
    }

}