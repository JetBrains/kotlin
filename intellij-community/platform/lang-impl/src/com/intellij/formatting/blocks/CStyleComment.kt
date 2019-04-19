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

import com.intellij.formatting.Block
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.formatter.common.AbstractBlock


class CStyleCommentBlock(comment: ASTNode, private val indent: Indent?): AbstractBlock(comment, null, null) {

  private val lines by lazy { lineBlocks() }
  val isCommentFormattable: Boolean by lazy {
    lines.drop(1).all { it.text.startsWith("*") }
  }

  val spacing: Spacing?
    get() = if (isCommentFormattable) null else Spacing.getReadOnlySpacing()

  override fun getSpacing(child1: Block?, child2: Block): Spacing? {
    val isLicenseComment = child1 == null && node.prev() == null
    if (isLicenseComment) {
      return Spacing.getReadOnlySpacing()
    }

    return child2.getSpacing(null, this)
  }

  override fun getIndent(): Indent? = indent

  override fun buildChildren(): List<Block> {
    if (!isCommentFormattable) return emptyList()

    return lines.map {
      val text = it.text
      val indent = when {
        !isCommentFormattable -> null
        text.startsWith("/*") -> Indent.getNoneIndent()
        else -> Indent.getSpaceIndent(1)
      }
      TextLineBlock(text, it.textRange, null, indent, null)
    }
  }


  private fun lineBlocks(): List<LineInfo> {
    return node.text
        .mapIndexed { index, char -> index to char }
        .split { it.second == '\n' }
        .mapNotNull {
          val block = it.dropWhile { Character.isWhitespace(it.second) }
          if (block.isEmpty()) return@mapNotNull null

          val text = block.map { it.second }.joinToString("").trimEnd()

          val startOffset = node.startOffset + block.first().first
          val range = TextRange(startOffset, startOffset + text.length)

          LineInfo(text, range)
        }
  }

  override fun isLeaf(): Boolean = !isCommentFormattable

}


private class LineInfo(val text: String, val textRange: TextRange)


fun <T> List<T>.split(predicate: (T) -> Boolean): List<List<T>> {
  if (indices.isEmpty()) return listOf()
  val result = mutableListOf<List<T>>()

  val current = mutableListOf<T>()
  for (e in this) {
    if (predicate(e)) {
      result.add(current.toList())
      current.clear()
    }
    else {
      current.add(e)
    }
  }

  if (current.isNotEmpty()) {
    result.add(current)
  }

  return result
}
