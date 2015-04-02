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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.j2k.ast.Element
import org.jetbrains.kotlin.j2k.usageProcessing.UsageProcessing
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.ArrayList
import java.util.HashMap

public trait ConversionScope {
    public fun contains(element: PsiElement): Boolean
}

public class FilesConversionScope(val files: Collection<PsiJavaFile>) : ConversionScope {
    override fun contains(element: PsiElement) = files.any { element.getContainingFile() == it }
}

public trait PostProcessor {
    public val contextToAnalyzeIn: PsiElement
    public fun analyzeFile(file: JetFile): BindingContext

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
                    val factory = JetPsiFactory(contextToAnalyzeIn.getProject())
                    property.getValOrVarNode().getPsi()!!.replace(factory.createVarNode().getPsi()!!)
                }
            }

            else -> null
        }
    }

    public fun doAdditionalProcessing(file: JetFile)
}

public class JavaToKotlinConverter(private val project: Project,
                                   private val settings: ConverterSettings,
                                   private val conversionScope: ConversionScope /*TODO: drop this parameter*/,
                                   private val referenceSearcher: ReferenceSearcher,
                                   private val resolverForConverter: ResolverForConverter) {
    private val LOG = Logger.getInstance("#org.jetbrains.kotlin.j2k.JavaToKotlinConverter")

    public fun elementsToKotlin(
            psiElementsAndProcessors: List<Pair<PsiElement, PostProcessor?>>,
            progress: ProgressIndicator = EmptyProgressIndicator()
    ): List<String> {
        try {
            val elementCount = psiElementsAndProcessors.size()
            val intermediateResults = ArrayList<((Map<PsiElement, UsageProcessing>) -> String)?>(elementCount)
            val usageProcessings = HashMap<PsiElement, UsageProcessing>()
            val usageProcessingCollector: (UsageProcessing) -> Unit = { usageProcessing ->
                assert(!usageProcessings.containsKey(usageProcessing.targetElement))
                    { "Duplicated UsageProcessing for target element ${usageProcessing.targetElement}" }
                usageProcessings.put(usageProcessing.targetElement, usageProcessing)
            }

            val progressText = "Converting Java to Kotlin"
            val fileCountText = elementCount.toString() + " " + if (elementCount > 1) "files" else "file"
            var fraction = 0.0
            var pass = 1

            fun processFilesWithProgress(passFraction: Double, processFile: (Int) -> Unit) {
                // we use special process with EmptyProgressIndicator to avoid changing text in our progress by inheritors search inside etc
                ProgressManager.getInstance().runProcess(
                        {
                            progress.setText("$progressText ($fileCountText) - pass $pass of 3")

                            val filesCount = psiElementsAndProcessors.indices
                            for (i in filesCount) {
                                progress.checkCanceled()
                                progress.setFraction(fraction + passFraction * i / elementCount)

                                val psiFile = psiElementsAndProcessors[i].first as? PsiFile
                                if (psiFile != null) {
                                    progress.setText2(psiFile.getVirtualFile().getPresentableUrl())
                                }

                                processFile(i)
                            }

                            pass++
                            fraction += passFraction
                        },
                        EmptyProgressIndicator())
            }

            processFilesWithProgress(0.25) { i ->
                val psiElement = psiElementsAndProcessors[i].first
                val postProcessor = psiElementsAndProcessors[i].second
                val converter = Converter.create(psiElement, settings, conversionScope, referenceSearcher, resolverForConverter, postProcessor, usageProcessingCollector)
                val result = converter.convert()
                intermediateResults.add(result)
            }

            val results = ArrayList<String>(elementCount)
            processFilesWithProgress(0.25) { i ->
                val result = intermediateResults[i]
                results.add(if (result != null) result(usageProcessings) else "")
                intermediateResults[i] = null // to not hold unused objects in the heap
            }

            val finalResults = ArrayList<String>(elementCount)
            processFilesWithProgress(0.5) { i ->
                val result = results[i]
                val postProcessor = psiElementsAndProcessors[i].second
                if (postProcessor != null) {
                    try {
                        finalResults.add(AfterConversionPass(project, postProcessor).run(result))
                    }
                    catch(e: ProcessCanceledException) {
                        throw e
                    }
                    catch(t: Throwable) {
                        LOG.error(t)
                        finalResults.add(result)
                    }
                }
                else {
                    finalResults.add(result)
                }
            }

            return finalResults
        }
        catch(e: ElementCreationStackTraceRequiredException) {
            // if we got this exception then we need to turn element creation stack traces on to get better diagnostic
            Element.saveCreationStacktraces = true
            try {
                return elementsToKotlin(psiElementsAndProcessors)
            }
            finally {
                Element.saveCreationStacktraces = false
            }
        }
    }
}
