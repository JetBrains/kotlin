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
package com.intellij.psi.formatter.common

import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.formatter.IndentRangesCalculator


class NodeIndentRangesCalculator(private val node: ASTNode) {

  fun calculateExtraRanges(): List<TextRange> {
    val document = retrieveDocument(node)
    if (document != null) {
      val ranges = node.textRange
      return IndentRangesCalculator(document, ranges).calcIndentRanges()
    }
    return listOf(node.textRange)
  }

  private fun retrieveDocument(node: ASTNode): Document? {
    val file = node.psi.containingFile
    return PsiDocumentManager.getInstance(node.psi.project).getDocument(file)
  }
  
}