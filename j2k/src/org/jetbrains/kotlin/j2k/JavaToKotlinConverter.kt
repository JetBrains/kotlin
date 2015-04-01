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
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
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

    public fun elementsToKotlin(psiElementsAndProcessors: List<Pair<PsiElement, PostProcessor?>>): List<String> {
        try {
            val intermediateResults = ArrayList<((Map<PsiElement, UsageProcessing>) -> String)?>(psiElementsAndProcessors.size())
            val usageProcessings = HashMap<PsiElement, UsageProcessing>()
            val usageProcessingCollector: (UsageProcessing) -> Unit = { usageProcessing ->
                assert(!usageProcessings.containsKey(usageProcessing.targetElement))
                    { "Duplicated UsageProcessing for target element ${usageProcessing.targetElement}" }
                usageProcessings.put(usageProcessing.targetElement, usageProcessing)
            }
            for ((psiElement, postProcessor) in psiElementsAndProcessors) {
                val converter = Converter.create(psiElement, settings, conversionScope, referenceSearcher, resolverForConverter, postProcessor, usageProcessingCollector)
                val result = converter.convert()
                intermediateResults.add(result)
            }

            val results = ArrayList<String>(psiElementsAndProcessors.size())
            for ((i, result) in intermediateResults.withIndex()) {
                results.add(if (result != null) result(usageProcessings) else "")
                intermediateResults[i] = null // to not hold unused objects in the heap
            }

            val finalResults = ArrayList<String>(psiElementsAndProcessors.size())
            for ((i, result) in results.withIndex()) {
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
