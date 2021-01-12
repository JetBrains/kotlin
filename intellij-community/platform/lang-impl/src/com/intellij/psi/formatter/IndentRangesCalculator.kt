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
package com.intellij.psi.formatter

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.util.text.CharArrayUtil

class IndentRangesCalculator(private val document: Document,
                             private val textRange: TextRange) 
{
  private val startOffset = textRange.startOffset
  private val endOffset = textRange.endOffset
  
  fun calcIndentRanges(): List<TextRange> {
    val startLine = document.getLineNumber(startOffset)
    val endLine = document.getLineNumber(endOffset)
    val chars = document.charsSequence
    
    val indentRanges = mutableListOf<TextRange>()
    
    for (line in startLine..endLine) {
      val lineStartOffset = document.getLineStartOffset(line)
      val lineEndOffset = document.getLineEndOffset(line)
      val firstNonWsChar = CharArrayUtil.shiftForward(chars, lineStartOffset, lineEndOffset + 1, " \t")
      indentRanges.add(TextRange(lineStartOffset, firstNonWsChar))
    }

    return indentRanges
  }
  
}