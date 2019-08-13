// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.formatting.blocks

import com.intellij.formatting.*
import com.intellij.openapi.util.TextRange

class TextLineBlock(private val range: TextRange, private val alignment: Alignment?, private val indent: Indent?, val spacing: Spacing?) : Block {
  override fun getTextRange(): TextRange = range

  override fun getSubBlocks(): List<Block> = emptyList()

  override fun getWrap(): Nothing? = null

  override fun getIndent(): Indent? = indent

  override fun getAlignment(): Alignment? = alignment

  override fun getSpacing(child1: Block?, child2: Block): Spacing? = spacing

  override fun getChildAttributes(newChildIndex: Int): ChildAttributes = throw UnsupportedOperationException("Should not be called")

  override fun isIncomplete(): Boolean = false

  override fun isLeaf(): Boolean = true
}