/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.conversion.copy.range
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.ConverterContext
import org.jetbrains.kotlin.j2k.PostProcessor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.AnalysisScope
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.NullabilityAnalysisFacade
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.nullabilityByUndefinedNullabilityComment
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.prepareTypeElementByMakingAllTypesNullableConsideringNullabilityComment
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import java.util.*

class NewJ2kPostProcessor(private val formatCode: Boolean) : PostProcessor {
    override fun insertImport(file: KtFile, fqName: FqName) {
        ApplicationManager.getApplication().invokeAndWait {
            runWriteAction {
                val descriptors = file.resolveImportReference(fqName)
                descriptors.firstOrNull()?.let { ImportInsertHelper.getInstance(file.project).importDescriptor(file, it) }
            }
        }
    }

    private enum class RangeFilterResult {
        SKIP,
        GO_INSIDE,
        PROCESS
    }

    private suspend fun List<NewJ2kPostProcessing>.runProcessings(
        file: KtFile,
        converterContext: NewJ2kConverterContext,
        rangeMarker: RangeMarker?
    ): Boolean {
        var modificationStamp: Long? = file.modificationStamp
        val elementToActions = runReadAction {
            collectAvailableActions(this, file, converterContext, rangeMarker)
        }
        withContext(EDT) {
            for ((element, action, _, writeActionNeeded) in elementToActions) {
                if (element.isValid) {
                    if (writeActionNeeded) {
                        runWriteAction {
                            runAction(action, element)
                        }
                    } else {
                        runAction(action, element)
                    }
                } else {
                    modificationStamp = null
                }
            }
        }

        return modificationStamp != file.modificationStamp && elementToActions.isNotEmpty()
    }

    private inline fun runAction(action: () -> Unit, element: PsiElement) {
        try {
            action()
        } catch (e: IllegalStateException) {
            element.containingFile.commitAndUnblockDocument()
            action()
        }
    }


    private suspend fun Processing.runProcessings(file: KtFile, converterContext: NewJ2kConverterContext, rangeMarker: RangeMarker?) {
        when (this) {
            is SingleOneTimeProcessing -> listOf(processing).runProcessings(file, converterContext, rangeMarker)
            is RepeatableProcessingGroup ->
                do {
                    val needContinue = processings.runProcessings(file, converterContext, rangeMarker)
                } while (needContinue)
            is OneTimeProcessingGroup ->
                processings.forEach { it.runProcessings(file, converterContext, rangeMarker) }
        }
    }


    override fun doAdditionalProcessing(file: KtFile, converterContext: ConverterContext?, rangeMarker: RangeMarker?) {
        runBlocking(EDT.ModalityStateElement(ModalityState.defaultModalityState())) {
            withContext(EDT) {
                NullabilityAnalysisFacade(
                    converterContext as NewJ2kConverterContext,
                    getTypeElementNullability = ::nullabilityByUndefinedNullabilityComment,
                    prepareTypeElement = ::prepareTypeElementByMakingAllTypesNullableConsideringNullabilityComment,
                    debugPrint = false
                ).fixNullability(AnalysisScope(file, rangeMarker))
            }
            withContext(EDT) {
                runWriteAction {
                    if (rangeMarker != null) {
                        ShortenReferences.DEFAULT.process(file, rangeMarker.startOffset, rangeMarker.endOffset)
                    } else {
                        ShortenReferences.DEFAULT.process(file)
                    }
                }
            }
            NewJ2KPostProcessingRegistrar.mainProcessings.runProcessings(file, converterContext as NewJ2kConverterContext, rangeMarker)
            withContext(EDT) {
                runWriteAction {
                    file.commitAndUnblockDocument()
                }
            }
            withContext(EDT) {
                runWriteAction {
                    val codeStyleManager = CodeStyleManager.getInstance(file.project)
                    if (rangeMarker != null) {
                        if (rangeMarker.isValid) {
                            codeStyleManager.reformatRange(file, rangeMarker.startOffset, rangeMarker.endOffset)
                        }
                    } else {
                        codeStyleManager.reformat(file)
                    }
                    Unit
                }
            }
        }
    }


    private data class ActionData(val element: PsiElement, val action: () -> Unit, val priority: Int, val writeActionNeeded: Boolean)

    private fun collectAvailableActions(
        processings: Collection<NewJ2kPostProcessing>,
        file: KtFile,
        context: NewJ2kConverterContext,
        rangeMarker: RangeMarker?
    ): List<ActionData> {
        val diagnostics = analyzeFileRange(file, rangeMarker)

        val availableActions = ArrayList<ActionData>()

        file.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val rangeResult = rangeFilter(element, rangeMarker)
                if (rangeResult == RangeFilterResult.SKIP) return

                super.visitElement(element)

                if (rangeResult == RangeFilterResult.PROCESS) {
                    processings.forEach { processing ->
                        val action = processing.createAction(element, diagnostics, context.converter.settings)
                        if (action != null) {
                            availableActions.add(
                                ActionData(
                                    element, action,
                                    NewJ2KPostProcessingRegistrar.priority(processing),
                                    processing.writeActionNeeded
                                )
                            )
                        }
                    }
                }
            }
        })
        availableActions.sortBy { it.priority }
        return availableActions
    }

    private fun analyzeFileRange(file: KtFile, rangeMarker: RangeMarker?): Diagnostics {
        val elements = if (rangeMarker == null)
            listOf(file)
        else
            file.elementsInRange(rangeMarker.range!!).filterIsInstance<KtElement>()

        return if (elements.isNotEmpty())
            file.getResolutionFacade().analyzeWithAllCompilerChecks(elements).bindingContext.diagnostics
        else
            Diagnostics.EMPTY
    }

    private fun rangeFilter(element: PsiElement, rangeMarker: RangeMarker?): RangeFilterResult {
        if (rangeMarker == null) return RangeFilterResult.PROCESS
        if (!rangeMarker.isValid) return RangeFilterResult.SKIP
        val range = TextRange(rangeMarker.startOffset, rangeMarker.endOffset)
        val elementRange = element.textRange
        return when {
            range.contains(elementRange) -> RangeFilterResult.PROCESS
            range.intersects(elementRange) -> RangeFilterResult.GO_INSIDE
            else -> RangeFilterResult.SKIP
        }
    }
}
