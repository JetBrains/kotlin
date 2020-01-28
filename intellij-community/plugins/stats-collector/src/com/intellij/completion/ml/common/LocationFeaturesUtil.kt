// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement

object LocationFeaturesUtil {
  private val LOG = Logger.getInstance(LocationFeaturesUtil.javaClass)

  fun indentLevel(line: String, tabSize: Int): Int {
    if (tabSize <= 0) return 0

    var indentLevel = 0
    var spaces = 0
    for (ch in line) {
      if (spaces == tabSize) {
        indentLevel += 1
        spaces = 0
      }

      if (ch == '\t') {
        indentLevel += 1
        spaces = 0
      }
      else if (ch == ' ') {
        spaces += 1
      }
      else {
        break
      }
    }

    return indentLevel
  }

  fun linesDiff(completionParameters: CompletionParameters, completionElement: PsiElement?): Int {

    if (completionElement == null) {
      return Int.MIN_VALUE
    }

    fun line(element: PsiElement?): Int? {
      if (element == null) {
        return null
      }

      val offset =
        if (completionParameters.position.containingFile == element.containingFile) {
          if (element.textOffset <= completionParameters.position.textOffset) element.textOffset
          else element.textOffset - DUMMY_IDENTIFIER.length
        }
        else if (completionParameters.originalFile == element.containingFile) element.textOffset
        else {
          return null
        }

      return completionParameters.editor.document.getLineNumber(offset)
    }

    try {
      val positionLine = line(completionParameters.position) ?: return Int.MIN_VALUE
      val elementLine = line(completionElement) ?: return Int.MIN_VALUE
      return positionLine - elementLine
    } catch (e: Throwable) {
      LOG.error("Error while calculating lines diff")
      return Int.MIN_VALUE
    }
  }
}