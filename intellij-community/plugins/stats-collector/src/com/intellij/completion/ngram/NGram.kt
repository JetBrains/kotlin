// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.completion.ngram.slp.modeling.Model
import com.intellij.completion.ngram.slp.modeling.ngram.JMModel
import com.intellij.completion.ngram.slp.modeling.runners.ModelRunner
import com.intellij.completion.ngram.slp.translating.Vocabulary
import com.intellij.lang.Language
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import kotlin.math.max


object NGram {
  internal val NGRAM_SCORER_KEY: Key<ScoringFunction> = Key.create("NGRAM_SCORER")
  private val SUPPORTED_LANGUAGES = setOf(
    "ecmascript 6",
    "go",
    "java",
    "kotlin",
    "php",
    "python",
    "ruby",
    "scala",
    "shell script"
    )

  private fun isSupported(language: Language): Boolean = language.id.toLowerCase() in SUPPORTED_LANGUAGES

  fun getNGramPrefix(parameters: CompletionParameters, order: Int): Array<String> {
    val precedingTokens = SyntaxTraverser.revPsiTraverser()
      .withRoot(parameters.originalFile)
      .onRange(TextRange(0, max(0, parameters.offset - 1)))
      .filter { shouldLex(it) }
      .take(order)
      .map { it.text }
      .reversed()
    if (precedingTokens.isEmpty()) return emptyArray()
      return with(precedingTokens) {
        if (last() == parameters.originalPosition?.text ?: "") dropLast(1) else drop(1)
      }.toTypedArray()
  }

  fun createScoringFunction(parameters: CompletionParameters, order: Int): ScoringFunction? {
    if (!isSupported(parameters.originalFile.language)) return null
    val modelRunner = createModelRunner()
    //modelRunner.setSelfTesting(true)
    modelRunner.learnPsiFile(parameters.originalFile)
    val prefix = getNGramPrefix(parameters, order)
    return ScoringFunction(prefix, modelRunner)
  }

  fun createModelRunner(
    vocabulary: Vocabulary = Vocabulary(),
    model: Model = JMModel()
  ): ModelRunner = ModelRunner(model, vocabulary)

  fun score(modelRunner: ModelRunner, tokens: List<String>): Double {
    return with(modelRunner) {
      val queryIndices = vocabulary.toIndices(tokens).filterNotNull()
      if (this.getSelfTesting()) model.forget(queryIndices)
      val probability = model.modelToken(queryIndices, queryIndices.size - 1).first
      if (this.getSelfTesting()) model.learn(queryIndices)
      probability
    }
  }

  private fun ModelRunner.learnPsiFile(file: PsiFile) {
    model.learn(vocabulary.toIndices(lexPsiFile(file)).filterNotNull())
  }

  private fun lexPsiFile(file: PsiFile): List<String> {
    return SyntaxTraverser.psiTraverser()
      .withRoot(file)
      .onRange(TextRange(0, 64 * 1024)) // first 128 KB of chars
      .filter { shouldLex(it) }
      .map { it.text }
      .toList()
  }

  private fun shouldLex(element: PsiElement): Boolean {
    return element.firstChild == null // is leaf
           && !element.text.isBlank()
           && element !is PsiComment
  }

  class ScoringFunction(prefix: Array<String>, private val modelRunner: ModelRunner) {
    private val tokens: MutableList<String> = mutableListOf(*prefix, "!placeholder!")

    fun score(value: String): Double {
      tokens[tokens.lastIndex] = value
      return score(modelRunner, (tokens))
    }
  }
}