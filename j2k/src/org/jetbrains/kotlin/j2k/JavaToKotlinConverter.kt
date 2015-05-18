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

package org.jetbrains.kotlin.j2k

import com.intellij.ide.util.DelegatingProgressIndicator
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.DummyHolder
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.JetLanguage
import org.jetbrains.kotlin.j2k.ast.Element
import org.jetbrains.kotlin.j2k.usageProcessing.UsageProcessing
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.ArrayList
import java.util.Comparator
import java.util.LinkedHashMap
import kotlin.properties.Delegates

public trait PostProcessor {
    public fun analyzeFile(file: JetFile, range: TextRange?): BindingContext

    public open fun fixForProblem(problem: Diagnostic): (() -> Unit)? {
        val psiElement = problem.getPsiElement()
        return when (problem.getFactory()) {
            Errors.UNNECESSARY_NOT_NULL_ASSERTION -> { ->
                val exclExclOp = psiElement as JetSimpleNameExpression
                val exclExclExpr = exclExclOp.getParent() as JetUnaryExpression
                exclExclExpr.replace(exclExclExpr.getBaseExpression()!!)
            }

            Errors.VAL_REASSIGNMENT -> { ->
                val property = (psiElement as? JetSimpleNameExpression)?.getReference()?.resolve() as? JetProperty
                if (property != null && !property.isVar()) {
                    val factory = JetPsiFactory(psiElement.getProject())
                    property.getValOrVarNode().getPsi()!!.replace(factory.createVarNode().getPsi()!!)
                }
            }

            else -> null
        }
    }

    public fun doAdditionalProcessing(file: JetFile, rangeMarker: RangeMarker?)
}

public enum class ParseContext {
    TOP_LEVEL,
    CODE_BLOCK
}

public class JavaToKotlinConverter(private val project: Project,
                                   private val settings: ConverterSettings,
                                   private val referenceSearcher: ReferenceSearcher,
                                   private val resolverForConverter: ResolverForConverter,
                                   private val postProcessor: PostProcessor?) {
    private val LOG = Logger.getInstance("#org.jetbrains.kotlin.j2k.JavaToKotlinConverter")

    public data class InputElement(
            val element: PsiElement,
            val postProcessingContext: PsiElement?
    )

    public data class ElementResult(val text: String,  val parseContext: ParseContext)

    public trait ExternalCodeProcessing {
        public fun prepareWriteOperation(progress: ProgressIndicator): () -> Unit
    }

    public data class Result(val results: List<ElementResult?>, val externalCodeProcessing: ExternalCodeProcessing?)

    public fun elementsToKotlin(
            inputElements: List<InputElement>,
            progress: ProgressIndicator = EmptyProgressIndicator()
    ): Result {
        try {
            val usageProcessings = LinkedHashMap<PsiElement, MutableCollection<UsageProcessing>>()
            val usageProcessingCollector: (UsageProcessing) -> Unit = {
                usageProcessings.getOrPut(it.targetElement, { ArrayList() }).add(it)
            }

            fun inConversionScope(element: PsiElement)
                    = inputElements.any { it.element.isAncestor(element, strict = false) }

            val progressText = "Converting Java to Kotlin"
            val elementCount = inputElements.size()
            val fileCountText = elementCount.toString() + " " + if (elementCount > 1) "files" else "file"
            var fraction = 0.0
            var pass = 1

            fun processFilesWithProgress<TInputItem, TOutputItem>(
                    fractionPortion: Double,
                    inputItems: Iterable<TInputItem>,
                    processItem: (TInputItem) -> TOutputItem
            ): List<TOutputItem> {
                val outputItems = ArrayList<TOutputItem>(elementCount)
                // we use special process with EmptyProgressIndicator to avoid changing text in our progress by inheritors search inside etc
                ProgressManager.getInstance().runProcess(
                        {
                            progress.setText("$progressText ($fileCountText) - pass $pass of 3")

                            for ((i, item) in inputItems.withIndex()) {
                                progress.checkCanceled()
                                progress.setFraction(fraction + fractionPortion * i / elementCount)

                                val psiFile = inputElements[i].element as? PsiFile
                                if (psiFile != null) {
                                    progress.setText2(psiFile.getVirtualFile().getPresentableUrl())
                                }

                                outputItems.add(processItem(item))
                            }

                            pass++
                            fraction += fractionPortion
                        },
                        EmptyProgressIndicator())
                return outputItems
            }

            val intermediateResults = processFilesWithProgress(0.25, inputElements) { inputElement ->
                Converter.create(inputElement.element, settings, ::inConversionScope, referenceSearcher, resolverForConverter, usageProcessingCollector).convert()
            }.toArrayList()

            val results = processFilesWithProgress(0.25, intermediateResults.withIndex()) { pair ->
                val (i, result) = pair
                intermediateResults[i] = null // to not hold unused objects in the heap
                if (result != null)
                    ElementResult(result.codeGenerator(usageProcessings), result.parseContext)
                else
                    null
            }

            val externalCodeProcessing = buildExternalCodeProcessing(usageProcessings, ::inConversionScope)

            if (postProcessor == null) {
                assert(progress is EmptyProgressIndicator, "Progress indicator not supported for postProcessor == null")
                return Result(results, externalCodeProcessing)
            }

            val finalResults = processFilesWithProgress(0.5, results.withIndex()) { pair ->
                val (i, result) = pair
                if (result != null) {
                    try {
                        //TODO: post processing does not work correctly for ParseContext different from TOP_LEVEL
                        val kotlinFile = JetPsiFactory(project).createAnalyzableFile("dummy.kt", result.text, inputElements[i].postProcessingContext!!)
                        AfterConversionPass(project, postProcessor).run(kotlinFile, range = null)
                        ElementResult(kotlinFile.getText(), result.parseContext)
                    }
                    catch(e: ProcessCanceledException) {
                        throw e
                    }
                    catch(t: Throwable) {
                        LOG.error(t)
                        result
                    }
                }
                else {
                    null
                }
            }

            return Result(finalResults, externalCodeProcessing)
        }
        catch(e: ElementCreationStackTraceRequiredException) {
            // if we got this exception then we need to turn element creation stack traces on to get better diagnostic
            Element.saveCreationStacktraces = true
            try {
                return elementsToKotlin(inputElements)
            }
            finally {
                Element.saveCreationStacktraces = false
            }
        }
    }

    data class ReferenceInfo(
            val reference: PsiReference,
            val target: PsiElement,
            val file: PsiFile,
            val processings: Collection<UsageProcessing>
    ) {
        val depth: Int by Delegates.lazy { target.parents(withItself = true).takeWhile { it !is PsiFile }.count() }
    }

    private fun buildExternalCodeProcessing(
            usageProcessings: Map<PsiElement, Collection<UsageProcessing>>,
            inConversionScope: (PsiElement) -> Boolean
    ): ExternalCodeProcessing? {
        if (usageProcessings.isEmpty()) return null

        val map: Map<PsiElement, Collection<UsageProcessing>> = usageProcessings.values()
                .flatMap { it }
                .filter { it.javaCodeProcessor != null || it.kotlinCodeProcessor != null }
                .groupBy { it.targetElement }
        if (map.isEmpty()) return null

        return object: ExternalCodeProcessing {
            override fun prepareWriteOperation(progress: ProgressIndicator): () -> Unit {
                val refs = ArrayList<ReferenceInfo>()

                progress.setText("Searching usages to update...")

                for ((i, entry) in map.entrySet().withIndex()) {
                    val psiElement = entry.key
                    val processings = entry.value

                    progress.setText2((psiElement as? PsiNamedElement)?.getName() ?: "")
                    progress.checkCanceled()

                    ProgressManager.getInstance().runProcess(
                            {
                                val searchJava = processings.any { it.javaCodeProcessor != null }
                                val searchKotlin = processings.any { it.kotlinCodeProcessor != null }
                                referenceSearcher.findUsagesForExternalCodeProcessing(psiElement, searchJava, searchKotlin)
                                        .filterNot { inConversionScope(it.getElement()) }
                                        .mapTo(refs) { ReferenceInfo(it, psiElement, it.getElement().getContainingFile(), processings) }
                            },
                            ProgressPortionReporter(progress, i / map.size().toDouble(), 1.0 / map.size()))

                }

                return { processUsages(refs) }
            }
        }
    }

    private fun processUsages(refs: Collection<ReferenceInfo>) {
        for (fileRefs in refs.groupBy { it.file }.values()) { // group by file for faster sorting
            ReferenceLoop@
            for ((reference, target, file, processings) in fileRefs.sortBy(ReferenceComparator)) {
                val processors = when (reference.getElement().getLanguage()) {
                    JavaLanguage.INSTANCE -> processings.map { it.javaCodeProcessor }.filterNotNull()
                    JetLanguage.INSTANCE -> processings.map { it.kotlinCodeProcessor }.filterNotNull()
                    else -> continue@ReferenceLoop
                }

                checkReferenceValid(reference)

                var references = listOf(reference)
                for (processor in processors) {
                    references = references.flatMap { processor.processUsage(it) ?: listOf(it) }
                    references.forEach { checkReferenceValid(it) }
                }
            }
        }
    }

    private fun checkReferenceValid(reference: PsiReference) {
        val element = reference.getElement()
        assert(element.isValid() && element.getContainingFile() !is DummyHolder) { "Reference $reference got invalidated" }
    }

    private object ReferenceComparator : Comparator<ReferenceInfo> {
        override fun compare(info1: ReferenceInfo, info2: ReferenceInfo): Int {
            val element1 = info1.reference.getElement()
            val element2 = info2.reference.getElement()

            val depth1 = info1.depth
            val depth2 = info2.depth
            if (depth1 != depth2) { // put deeper elements first to not invalidate them when processing ancestors
                return -depth1.compareTo(depth2)
            }

            // process elements with the same parent from right to left so that right-side of assignments is not invalidated by processing of the left one
            return -element1.getStartOffsetInParent().compareTo(element2.getStartOffsetInParent())
        }
    }

    private class ProgressPortionReporter(
            indicator: ProgressIndicator,
            private val start: Double,
            private val portion: Double
    ) : DelegatingProgressIndicator(indicator) {

        init {
            setFraction(0.0)
        }

        override fun start() {
            setFraction(0.0)
        }

        override fun stop() {
            setFraction(portion)
        }

        override fun setFraction(fraction: Double) {
            super.setFraction(start + (fraction * portion))
        }

        override fun getFraction(): Double {
            return (super.getFraction() - start) / portion
        }

        override fun setText(text: String?) {
        }

        override fun setText2(text: String?) {
        }
    }
}
