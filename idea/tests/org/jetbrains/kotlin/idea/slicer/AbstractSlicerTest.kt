/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
import com.intellij.util.PathUtil
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

            fun SliceNode.isSliceLeafValueClassNode() = javaClass.name == "com.intellij.slicer.SliceLeafValueClassNode"

            fun process(node: SliceNode, indent: Int): String {
                val usage = node.element!!.value

                node.calculateDupNode()
                val isDuplicated = !node.isSliceLeafValueClassNode() && node.duplicate != null

                return buildString {
                    when {
                        node is SliceRootNode && usage.element is KtFile -> {
                            node.sortedChildren.forEach { append(process(it, indent)) }
                            return@buildString
                        }
                        // SliceLeafValueClassNode is package-private
                        node.isSliceLeafValueClassNode() -> append("[${node.nodeText}]\n")
                        else -> {
                            val chunks = usage.text
                            append(chunks.first().render() + " ")
                            append("\t".repeat(indent))
                            if (usage is KotlinSliceDereferenceUsage) {
                                append("DEREFERENCE: ")
                            }
                            if (usage is KotlinSliceUsage) {
                                append("[LAMBDA] ".repeat(usage.lambdaLevel))
                            }
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
        val extraFiles = rootDir.listFiles { _, name ->
            name.startsWith("$namePrefix.") && PathUtil.getFileExtension(name).let { it == "kt" || it == "java" }
        }

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
        val elementAtCaret = file.findElementAt(editor.caretModel.offset)!!
        val sliceProvider = KotlinSliceProvider()
        val expression = sliceProvider.getExpressionAtCaret(elementAtCaret, analysisParams.dataFlowToThis)!!
        val rootUsage = sliceProvider.createRootUsage(expression, analysisParams)
        val rootNode = SliceRootNode(project, DuplicateMap(), rootUsage)
        doTest(path, sliceProvider, rootNode)
    }
}