// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.completion.ngram.slp.modeling.Model
import com.intellij.completion.ngram.slp.modeling.ngram.JMModel
import com.intellij.completion.ngram.slp.modeling.ngram.NGramModel
import com.intellij.completion.ngram.slp.modeling.runners.ModelRunner
import com.intellij.completion.ngram.slp.translating.Vocabulary
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser


fun makeModelRunner(
  vocabulary: Vocabulary = Vocabulary(),
  model: Model = JMModel()
): ModelRunner = ModelRunner(model,
                                                                                                                                   vocabulary)


fun ModelRunner.getNgramProbability(ngram: Ngram): Double {
  val queryIndices = vocabulary.toIndices(ngram.tokens).filterNotNull()
  if (this.getSelfTesting()) model.forget(queryIndices)
  val probability = model.modelToken(queryIndices, queryIndices.size - 1).first
  if (this.getSelfTesting()) model.learn(queryIndices)
  return probability
}

fun ModelRunner.getTokenCount(token: String): Int =
  (model as? NGramModel)?.counter?.getCounts(vocabulary.toIndices(listOf(token)))?.first()?.toInt() ?: -1

fun ModelRunner.learnTokens(tokens: List<String>) {
  model.learn(vocabulary.toIndices(tokens).filterNotNull())
}

fun ModelRunner.learnPsiFile(file: PsiFile) {
  model.learn(vocabulary.toIndices(lexPsiFile(file)).filterNotNull())
}

fun lexPsiFile(file: PsiFile): List<String> {
  return SyntaxTraverser.psiTraverser()
    .withRoot(file)
    .onRange(TextRange(0, 512 * 1024)) // first 1MB of chars
    .filter { shouldLex(it) }
    .map { it.text }
    .toList()
}

fun shouldLex(element: PsiElement): Boolean {
  return element.firstChild == null // is leaf
         && !element.text.isBlank()
         && element !is PsiComment
}