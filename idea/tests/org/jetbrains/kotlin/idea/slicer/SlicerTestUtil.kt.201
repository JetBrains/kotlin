/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.analysis.AnalysisScope
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.util.treeView.AbstractTreeStructureBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.slicer.DuplicateMap
import com.intellij.slicer.SliceAnalysisParams
import com.intellij.slicer.SliceNode
import com.intellij.slicer.SliceRootNode
import com.intellij.usages.TextChunk
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.contains
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.awt.Font

internal class TestSliceTreeStructure(private val rootNode: SliceNode) : AbstractTreeStructureBase(rootNode.project) {
    override fun getProviders() = emptyList<TreeStructureProvider>()

    override fun getRootElement() = rootNode

    override fun commit() {
    }

    override fun hasSomethingToCommit() = false
}

internal fun buildTreeRepresentation(rootNode: SliceNode): String {
    val project = rootNode.element!!.project!!
    val projectScope = GlobalSearchScope.projectScope(project)

    fun TextChunk.render(): String {
        var text = text
        if (attributes.fontType == Font.BOLD) {
            text = "<bold>$text</bold>"
        }
        return text
    }

    fun SliceNode.isSliceLeafValueClassNode() = this is HackedSliceLeafValueClassNode

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
                    if (!projectScope.contains(usage.element)) {
                        append("LIB ")
                    } else {
                        append(chunks.first().render() + " ")
                    }

                    repeat(indent) { append('\t') }

                    if (usage is KotlinSliceDereferenceUsage) {
                        append("DEREFERENCE: ")
                    }

                    if (usage is KotlinSliceUsage) {
                        usage.mode.inlineCallStack.forEach {
                            append("(INLINE CALL ${it.function?.name}) ")
                        }
                        usage.mode.behaviourStack.reversed().joinTo(this, separator = "") { it.testPresentationPrefix }
                    }

                    if (isDuplicated) {
                        append("DUPLICATE: ")
                    }

                    chunks.slice(1 until chunks.size).joinTo(this, separator = "") { it.render() }

                    KotlinSliceUsageCellRenderer.containerSuffix(usage)?.let {
                        append(" ($it)")
                    }

                    append("\n")
                }
            }

            if (!isDuplicated) {
                node.sortedChildren.forEach { append(process(it, indent + 1)) }
            }
        }.replace(Regex("</bold><bold>"), "")
    }

    return process(rootNode, 0)
}

private val SliceNode.sortedChildren: List<SliceNode>
    get() = children.sortedBy { it.value.element?.startOffset ?: -1 }

internal fun testSliceFromOffset(
    file: KtFile,
    offset: Int,
    doTest: (sliceProvider: KotlinSliceProvider, rootNode: SliceRootNode) -> Unit
) {
    val fileText = file.text
    val flowKind = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// FLOW: ")
    val withDereferences = InTextDirectivesUtils.isDirectiveDefined(fileText, "// WITH_DEREFERENCES")
    val analysisParams = SliceAnalysisParams().apply {
        dataFlowToThis = when (flowKind) {
            "IN" -> true
            "OUT" -> false
            else -> throw AssertionError("Invalid flow kind: $flowKind")
        }
        showInstanceDereferences = withDereferences
        scope = AnalysisScope(file.project)
    }

    val elementAtCaret = file.findElementAt(offset)!!
    val sliceProvider = KotlinSliceProvider()
    val expression = sliceProvider.getExpressionAtCaret(elementAtCaret, analysisParams.dataFlowToThis)!!
    val rootUsage = sliceProvider.createRootUsage(expression, analysisParams)
    val rootNode = SliceRootNode(file.project, DuplicateMap(), rootUsage)
    doTest(sliceProvider, rootNode)
}