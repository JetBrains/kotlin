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

package org.jetbrains.kotlin.idea.slicer

import com.intellij.analysis.AnalysisScope
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.slicer.SliceAnalysisParams
import com.intellij.slicer.SliceUsage
import com.intellij.usages.TextChunk
import com.intellij.util.CommonProcessors
import gnu.trove.TObjectHashingStrategy
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.awt.Font
import java.io.File

abstract class AbstractSlicerTest : KotlinLightCodeInsightFixtureTestCase() {
    object SliceUsageHashingStrategy : TObjectHashingStrategy<SliceUsage> {
        override fun computeHashCode(`object`: SliceUsage) = `object`.usageInfo.hashCode()
        override fun equals(o1: SliceUsage, o2: SliceUsage) = o1.usageInfo == o2.usageInfo
    }

    // Based on SliceUsage.processChildren
    private fun KotlinSliceUsage.processChildrenWithoutProgress(processor: (KotlinSliceUsage) -> Unit) {
        val element = runReadAction { element }

        val uniqueProcessor = CommonProcessors.UniqueProcessor(
                {
                    processor(it as KotlinSliceUsage)
                    true
                },
                SliceUsageHashingStrategy
        )

        runReadAction {
            if (params.dataFlowToThis) {
                processUsagesFlownDownTo(element, uniqueProcessor)
            }
            else {
                processUsagesFlownFromThe(element, uniqueProcessor)
            }
        }
    }

    private fun buildTreeRepresentation(rootUsage: KotlinSliceUsage): String {
        val visitedElements = HashSet<PsiElement>()

        fun TextChunk.render(): String {
            var text = text
            if (attributes.fontType == Font.BOLD) {
                text = "<bold>$text</bold>"
            }
            return text
        }

        fun process(usage: KotlinSliceUsage, indent: Int): String {
            val isDuplicated = !visitedElements.add(usage.element)

            return buildString {
                val chunks = usage.text
                append(chunks.first().render() + " ")
                append("\t".repeat(indent))
                chunks.slice(1..chunks.size - 1).joinTo(
                        this,
                        separator = "",
                        prefix = if (isDuplicated) "DUPLICATE: " else "",
                        postfix = "\n"
                ) { it.render() }
                if (!isDuplicated) {
                    usage.processChildrenWithoutProgress { append(process(it, indent + 1)) }
                }
            }.replace(Regex("</bold><bold>"), "")
        }

        return process(rootUsage, 0)
    }

    protected fun doTest(path: String) {
        val mainFile = File(path)

        myFixture.testDataPath = "${KotlinTestUtils.getHomeDirectory()}/${mainFile.parent}"

        val fileText = FileUtil.loadFile(mainFile, true)
        val flowKind = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// FLOW: ")
        val analysisParams = SliceAnalysisParams().apply {
            dataFlowToThis = when (flowKind) {
                "IN" -> true
                "OUT" -> false
                else -> throw AssertionError("Invalid flow kind: $flowKind")
            }
            scope = AnalysisScope(project)
        }

        val file = myFixture.configureByFile(mainFile.name) as KtFile
        val elementAtCaret = file.findElementAt(editor.caretModel.offset)
        val sliceProvider = KotlinSliceProvider()
        val expression = sliceProvider.getExpressionAtCaret(elementAtCaret, analysisParams.dataFlowToThis)!!
        val rootUsage = sliceProvider.createRootUsage(expression, analysisParams)
        KotlinTestUtils.assertEqualsToFile(File(path.replace(".kt", ".results.txt")), buildTreeRepresentation(rootUsage))
    }
}