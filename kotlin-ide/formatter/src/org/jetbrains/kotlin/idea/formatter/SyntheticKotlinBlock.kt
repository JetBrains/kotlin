/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange

class SyntheticKotlinBlock(
    private val node: ASTNode,
    private val subBlocks: List<ASTBlock>,
    private val alignment: Alignment?,
    private val indent: Indent?,
    private val wrap: Wrap?,
    private val spacingBuilder: KotlinSpacingBuilder,
    private val createParentSyntheticSpacingBlock: (ASTNode) -> ASTBlock
) : ASTBlock {

    private val textRange = TextRange(
        subBlocks.first().textRange.startOffset,
        subBlocks.last().textRange.endOffset
    )

    override fun getTextRange(): TextRange = textRange
    override fun getSubBlocks() = subBlocks
    override fun getWrap() = wrap
    override fun getIndent() = indent
    override fun getAlignment() = alignment
    override fun getChildAttributes(newChildIndex: Int) = ChildAttributes(getIndent(), null)
    override fun isIncomplete() = getSubBlocks().last().isIncomplete
    override fun isLeaf() = false
    override fun getNode() = node
    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingBuilder.getSpacing(createParentSyntheticSpacingBlock(node), child1, child2)
    }


    override fun toString(): String {
        var child = subBlocks.first()
        var treeNode: ASTNode? = null

        loop@
        while (treeNode == null) when (child) {
            is SyntheticKotlinBlock -> child = child.getSubBlocks().first()

            else -> treeNode = child.node
        }

        val textRange = getTextRange()
        val psi = treeNode.psi
        if (psi != null) {
            val file = psi.containingFile
            if (file != null) {
                return file.text!!.subSequence(textRange.startOffset, textRange.endOffset).toString() + " " + textRange
            }
        }

        return this::class.java.name + ": " + textRange
    }
}