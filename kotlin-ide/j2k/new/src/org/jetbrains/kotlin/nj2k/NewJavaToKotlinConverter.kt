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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.conversions.JKResolver
import org.jetbrains.kotlin.nj2k.externalCodeProcessing.NewExternalCodeProcessing
import org.jetbrains.kotlin.nj2k.printing.JKCodeBuilder
import org.jetbrains.kotlin.nj2k.types.JKTypeFactory
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
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
    ): FilesResult = filesToKotlin(files, postProcessor, progress, null)

    fun filesToKotlin(
        files: List<PsiJavaFile>,
        postProcessor: PostProcessor,
        progress: ProgressIndicator,
        bodyFilter: ((PsiElement) -> Boolean)?,
    ): FilesResult {
        progress.isIndeterminate = false
        val phasesCount = postProcessor.phasesCount + 1
        val withProgressProcessor = NewJ2kWithProgressProcessor(progress, files, phasesCount)
        return withProgressProcessor.process {
            val (results, externalCodeProcessing, context) =
                ApplicationManager.getApplication().runReadAction(Computable {
                    elementsToKotlin(files, withProgressProcessor, bodyFilter)
                })

            val kotlinFiles = results.mapIndexed { i, result ->
                runUndoTransparentActionInEdt(inWriteAction = true) {
                    val javaFile = files[i]
                    KtPsiFactory(project).createFileWithLightClassSupport(
                        javaFile.name.replace(".java", ".kt"),
                        result!!.text,
                        files[i]
                    ).apply {
                        addImports(result.importsToAdd)
                    }
                }

            }

            postProcessor.doAdditionalProcessing(
                JKMultipleFilesPostProcessingTarget(kotlinFiles),
                context
            ) { phase, description ->
                withProgressProcessor.updateState(fileIndex = null, phase = phase + 1, description = description)
            }
            FilesResult(kotlinFiles.map { it.text }, externalCodeProcessing)
        }
    }

    fun elementsToKotlin(inputElements: List<PsiElement>, bodyFilter: ((PsiElement) -> Boolean)?): Result {
        return elementsToKotlin(inputElements, NewJ2kWithProgressProcessor.DEFAULT, bodyFilter)
    }

    fun elementsToKotlin(inputElements: List<PsiElement>, processor: WithProgressProcessor, bodyFilter: ((PsiElement) -> Boolean)?): Result {
        val phaseDescription = KotlinNJ2KBundle.message("phase.converting.j2k")
        val contextElement = inputElements.firstOrNull() ?: return Result(emptyList(), null, null)
        val resolver = JKResolver(project, targetModule, contextElement)
        val symbolProvider = JKSymbolProvider(resolver)
        val typeFactory = JKTypeFactory(symbolProvider)
        symbolProvider.typeFactory = typeFactory
        symbolProvider.preBuildTree(inputElements)

        val languageVersion = when {
            contextElement.isPhysical -> contextElement.languageVersionSettings
            else -> LanguageVersionSettingsImpl.DEFAULT
        }

        val importStorage = JKImportStorage(languageVersion)
        val treeBuilder = JavaToJKTreeBuilder(symbolProvider, typeFactory, converterServices, importStorage, bodyFilter)

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
        val inConversionContext = { element: PsiElement ->
            inputElements.any { inputElement ->
                if (inputElement == element) return@any true
                inputElement.isAncestor(element, true)
            }
        }

        val externalCodeProcessing =
            NewExternalCodeProcessing(oldConverterServices.referenceSearcher, inConversionContext)

        val context = NewJ2kConverterContext(
            symbolProvider,
            typeFactory,
            this,
            inConversionContext,
            importStorage,
            JKElementInfoStorage(),
            externalCodeProcessing,
            languageVersion.supportsFeature(LanguageFeature.FunctionalInterfaceConversion)
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

        return Result(
            results,
            externalCodeProcessing.takeIf { it.isExternalProcessingNeeded() },
            context
        )
    }

    override fun elementsToKotlin(inputElements: List<PsiElement>): Result {
        return elementsToKotlin(inputElements, NewJ2kWithProgressProcessor.DEFAULT)
    }

    override fun elementsToKotlin(inputElements: List<PsiElement>, processor: WithProgressProcessor): Result {
        return elementsToKotlin(inputElements, processor, null)
    }

    companion object {
        fun KtFile.addImports(imports: Collection<FqName>) {
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

    override fun updateState(fileIndex: Int?, phase: Int, description: String) {
        progress?.checkCanceled()
        progress?.fraction = phase / phasesCount.toDouble()
        progress?.text = KotlinNJ2KBundle.message("progress.text", description, phase, phasesCount)
        progress?.text2 = when {
            files != null && files.isNotEmpty() && fileIndex != null -> files[fileIndex].virtualFile.presentableUrl
            else -> ""
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
