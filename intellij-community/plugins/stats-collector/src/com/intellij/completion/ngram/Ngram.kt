// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.SyntaxTraverser


class Ngram(tokens: List<String>, val order: Int) {

  val tokens = tokens.takeLast(order)

  companion object {
    internal val NGRAM_PREFIX_KEY: Key<Array<String>> = Key.create("ngramPrefix")

    fun getNgramPrefix(parameters: CompletionParameters, order: Int): Array<String> {
      val precedingTokens = SyntaxTraverser.revPsiTraverser()
        .withRoot(parameters.originalFile)
        .onRange(TextRange(0, parameters.offset - 1))
        .filter { shouldLex(it) }
        .take(order)
        .map { it.text }
        .reversed()
      return with(precedingTokens) {
        if (last() == parameters.originalPosition?.text ?: "") dropLast(1) else drop(1)
      }.toTypedArray()
    }

  }
}