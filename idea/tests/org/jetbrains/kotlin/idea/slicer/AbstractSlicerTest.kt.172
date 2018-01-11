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
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.util.treeView.AbstractTreeStructureBase
import com.intellij.openapi.util.io.FileUtil
import com.intellij.slicer.DuplicateMap
import com.intellij.slicer.SliceAnalysisParams
import com.intellij.slicer.SliceNode
import com.intellij.slicer.SliceRootNode
import com.intellij.usages.TextChunk
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.awt.Font
import java.io.File

abstract class AbstractSlicerTest : KotlinLightCodeInsightFixtureTestCase() {
    protected class SliceTreeStructure(private val rootNode: SliceNode) : AbstractTreeStructureBase(rootNode.project) {
        override fun getProviders(): List<TreeStructureProvider>? = emptyList()

        override fun getRootElement() = rootNode

        override fun commit() {

        }

        override fun hasSomethingToCommit() = false
    }

    companion object {
        private val SliceNode.sortedChildren : List<SliceNode>
            get() = children.sortedBy { it.value.element?.startOffset ?: -1 }

        @JvmStatic
        protected fun buildTreeRepresentation(rootNode: SliceNode): String {
            fun TextChunk.render(): String {
                var text = text
                if (attributes.fontType == Font.BOLD) {
                    text = "<bold>$text</bold>"
                }
                return text
            }

            fun process(node: SliceNode, indent: Int): String {
                val usage = node.element!!.value as KotlinSliceUsage

                node.calculateDupNode()
                val isDuplicated = node.duplicate != null

                return buildString {
                    when {
                        node is SliceRootNode && usage.element is KtFile -> {
                            node.sortedChildren.forEach { append(process(it, indent)) }
                            return@buildString
                        }
                        else -> {
                            val chunks = usage.text
                            append(chunks.first().render() + " ")
                            append("\t".repeat(indent))
                            if (usage is KotlinSliceDereferenceUsage) {
                                append("DEREFERENCE: ")
                            }
                            append("[LAMBDA] ".repeat(usage.lambdaLevel))
                            chunks.slice(1..chunks.size - 1).joinTo(
                                    this,
                                    separator = "",
                                    prefix = if (isDuplicated) "DUPLICATE: " else "",
                                    postfix = "\n"
                            ) { it.render() }
                        }
                    }

                    if (!isDuplicated) {
                        node.sortedChildren.forEach { append(process(it, indent + 1)) }
                    }
                }.replace(Regex("</bold><bold>"), "")
            }

            return process(rootNode, 0)
        }
    }

    protected abstract fun doTest(path: String, sliceProvider: KotlinSliceProvider, rootNode: SliceRootNode)

    protected fun doTest(path: String) {
        val mainFile = File(path)
        val rootDir = mainFile.parentFile

        val namePrefix = FileUtil.getNameWithoutExtension(mainFile)
        val extraFiles = rootDir.listFiles { _, name -> name.startsWith("$namePrefix.") }

        myFixture.testDataPath = "${KotlinTestUtils.getHomeDirectory()}/${rootDir.path}"

        val fileText = FileUtil.loadFile(mainFile, true)
        val flowKind = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// FLOW: ")
        val withDereferences = InTextDirectivesUtils.isDirectiveDefined(fileText, "// WITH_DEREFERENCES")
        val analysisParams = SliceAnalysisParams().apply {
            dataFlowToThis = when (flowKind) {
                "IN" -> true
                "OUT" -> false
                else -> throw AssertionError("Invalid flow kind: $flowKind")
            }
            showInstanceDereferences = withDereferences
            scope = AnalysisScope(project)
        }

        extraFiles.forEach { myFixture.configureByFile(it.name) }
        val file = myFixture.configureByFile(mainFile.name) as KtFile
        val elementAtCaret = file.findElementAt(editor.caretModel.offset)
        val sliceProvider = KotlinSliceProvider()
        val expression = sliceProvider.getExpressionAtCaret(elementAtCaret, analysisParams.dataFlowToThis)!!
        val rootUsage = sliceProvider.createRootUsage(expression, analysisParams)
        val rootNode = SliceRootNode(project, DuplicateMap(), rootUsage)
        doTest(path, sliceProvider, rootNode)
    }
}