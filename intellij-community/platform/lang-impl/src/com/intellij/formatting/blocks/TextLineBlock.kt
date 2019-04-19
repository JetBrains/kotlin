/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.formatting.blocks

import com.intellij.formatting.*
import com.intellij.openapi.util.TextRange

class TextLineBlock(
  val text: String,
  private val textRange: TextRange,
  private val alignment: Alignment?,
  private val indent: Indent?,
  val spacing: Spacing?
) : Block {

  override fun getTextRange(): TextRange {
    return textRange
  }

  override fun getSubBlocks(): List<Block> = emptyList()

  override fun getWrap(): Nothing? = null

  override fun getIndent(): Indent? = indent

  override fun getAlignment(): Alignment? = alignment

  override fun getSpacing(child1: Block?, child2: Block): Spacing? = spacing

  override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
    throw UnsupportedOperationException("Should not be called")
  }

  override fun isIncomplete(): Boolean = false

  override fun isLeaf(): Boolean = true

  override fun toString(): String {
    return "TextLineBlock(text='$text', textRange=$textRange)"
  }

}
