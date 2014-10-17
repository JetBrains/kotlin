/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k

import com.intellij.psi.PsiElement
import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.jet.j2k.ast.Element
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.BindingContext
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jet.j2k.usageProcessing.UsageProcessingExpressionConverter

public trait ConversionScope {
    public fun contains(element: PsiElement): Boolean
}

public class FilesConversionScope(val files: Collection<PsiJavaFile>) : ConversionScope {
    override fun contains(element: PsiElement) = files.any { element.getContainingFile() == it }
}

public trait PostProcessor {
    public val contextToAnalyzeIn: PsiElement
    public fun analyzeFile(file: JetFile): BindingContext
    public fun doAdditionalProcessing(file: JetFile)
}

public class JavaToKotlinConverter(private val project: Project,
                                   private val settings: ConverterSettings,
                                   private val conversionScope: ConversionScope,
                                   private val referenceSearcher: ReferenceSearcher,
                                   private val postProcessor: PostProcessor?) {
    private val LOG = Logger.getInstance("#org.jetbrains.jet.j2k.JavaToKotlinConverter")

    //TODO: run not for one element when multiple files
    public fun elementToKotlin(psiElement: PsiElement): String {
        try {
            val converter = Converter.create(psiElement, settings, conversionScope, referenceSearcher, postProcessor)
            val element = converter.convert() ?: return ""

            val codeConverter = converter.createDefaultCodeConverter()
                    .withSpecialExpressionConverter(UsageProcessingExpressionConverter(converter.usageProcessings))

            converter.unfoldLazyElements(codeConverter)

            val builder = CodeBuilder(psiElement)
            builder.append(element)

            if (postProcessor != null) {
                try {
                    return AfterConversionPass(project, postProcessor).run(builder.result)
                }
                catch(e: ProcessCanceledException) {
                    throw e
                }
                catch(t: Throwable) {
                    LOG.error(t)
                    return builder.result
                }
            }
            else {
                return builder.result
            }
        }
        catch(e: ElementCreationStackTraceRequiredException) {
            // if we got this exception then we need to turn element creation stack traces on to get better diagnostic
            Element.saveCreationStacktraces = true
            try {
                return elementToKotlin(psiElement)
            }
            finally {
                Element.saveCreationStacktraces = false
            }
        }
    }
}