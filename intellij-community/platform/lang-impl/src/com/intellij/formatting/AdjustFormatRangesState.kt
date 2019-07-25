/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.formatting

import com.intellij.formatting.engine.State
import com.intellij.openapi.util.TextRange
import com.intellij.psi.formatter.common.ExtraRangesProvider
import com.intellij.util.containers.Stack

class AdjustFormatRangesState(var currentRoot: Block, val formatRanges: FormatTextRanges) : State() {
  private val extendedRanges = formatRanges.extendedFormattingRanges
  private val totalNewRanges = mutableListOf<TextRange>()
  private val state = Stack(currentRoot)

  init {
    setOnDone({
      totalNewRanges.forEach {
        formatRanges.add(it, false)
      }
    })
  }

  override fun doIteration() {
    val currentBlock = state.pop()
    processBlock(currentBlock)
    isDone = state.isEmpty()
  }

  private fun processBlock(currentBlock: Block) {
    if (!isInsideExtendedFormattingRanges(currentBlock)) return

    currentBlock.subBlocks
          .reversed()
          .forEach { state.push(it) }

    if (!formatRanges.isReadOnly(currentBlock.textRange)) {
      extractRanges(currentBlock)
    }
  }

  private fun isInsideExtendedFormattingRanges(currentBlock: Block): Boolean {
    val textRange = currentBlock.textRange
    return extendedRanges.find { it.intersects(textRange) } != null
  }

  private fun extractRanges(block: Block) {
    if (block is ExtraRangesProvider) {
      val newRanges = block.getExtraRangesToFormat(formatRanges)
      if (newRanges != null) {
        totalNewRanges.addAll(newRanges)
      }
    }
  }


}