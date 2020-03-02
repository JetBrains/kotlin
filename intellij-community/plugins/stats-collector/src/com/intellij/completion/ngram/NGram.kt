// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.codeInsight.completion.CompletionParameters
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
import com.intellij.stats.CompletionStatsPolicy
import kotlin.math.max
import kotlin.math.min


object NGram {
  internal val NGRAM_SCORER_KEY: Key<Scorer> = Key.create("NGRAM_SCORER")
  internal val NGRAM_REVERSED_SCORER_KEY: Key<Scorer> = Key.create("NGRAM_REVERSED_SCORER")

  @Deprecated("Use CompletionStatsPolicy instead")
  private val SUPPORTED_LANGUAGES = setOf(
    "ecmascript 6",
    "go",
    "java",
    "kotlin",
    "php",
    "python",
    "ruby",
    "scala",
    "shell script",
    "objectivec"
  )
  private const val TEXT_RANGE_LIMIT = 64 * 1024 // 128 KB of chars

  internal fun getScorers(parameters: CompletionParameters, order: Int): Map<Key<Scorer>, Scorer> {
    val language = parameters.originalFile.language
    if (!isSupported(language)) return emptyMap()
    val prefix = getNGramPrefix(parameters, order)
    val reversedPostfix = getNGramReversedPostfix(parameters, order)
    val lexedFile = lexPsiFile(parameters.originalFile)
    return mapOf(NGRAM_SCORER_KEY to createScorer(prefix, lexedFile),
                 NGRAM_REVERSED_SCORER_KEY to createReversedScorer(reversedPostfix, lexedFile))
  }

  private fun createScorer(prefix: Array<String>, lexedFile: List<String>): Scorer {
    val modelRunner = createModelRunner(lexedFile)
    return Scorer({ modelRunner.score(it) }, prefix)
  }

  private fun createReversedScorer(reversedPostfix: Array<String>, lexedFile: List<String>): Scorer {
    val modelRunner = createModelRunner(lexedFile.reversed())
    return Scorer({ modelRunner.score(it) }, reversedPostfix)
  }

  private fun isSupported(language: Language): Boolean = language.id.toLowerCase() in SUPPORTED_LANGUAGES
                                                         || CompletionStatsPolicy.useNgramModel(language)

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

  fun getNGramReversedPostfix(parameters: CompletionParameters, order: Int): Array<String> {
    val followingTokens = SyntaxTraverser.psiTraverser()
      .withRoot(parameters.originalFile)
      .onRange(
        TextRange(min(parameters.offset + (parameters.originalPosition?.textRange?.length ?: 0), TEXT_RANGE_LIMIT - 1), TEXT_RANGE_LIMIT))
      .filter { shouldLex(it) }
      .take(order)
      .map { it.text }
      .reversed()
    if (followingTokens.isEmpty()) return emptyArray()
    return with(followingTokens) {
      if (last() == parameters.originalPosition?.text ?: "") dropLast(1) else drop(1)
    }.toTypedArray()
  }

  fun createModelRunner(tokens: List<String>): ModelRunner = ModelRunner(JMModel(), Vocabulary()).apply { learnTokens(tokens) }

  private fun ModelRunner.learnTokens(tokens: List<String>) {
    model.learn(vocabulary.toIndices(tokens).filterNotNull())
  }

  fun ModelRunner.score(tokens: List<String>): Double {
    val queryIndices = vocabulary.toIndices(tokens).filterNotNull()
    return model.modelToken(queryIndices, queryIndices.size - 1).first
  }

  private fun lexPsiFile(file: PsiFile): List<String> {
    return SyntaxTraverser.psiTraverser()
      .withRoot(file)
      .onRange(TextRange(0, TEXT_RANGE_LIMIT))
      .filter { shouldLex(it) }
      .map { it.text }
      .toList()
  }

  private fun shouldLex(element: PsiElement): Boolean {
    return element.firstChild == null // is leaf
           && !element.text.isBlank()
           && element !is PsiComment
  }

  internal class Scorer(private val scoringFunction: (List<String>) -> Double, prefix: Array<String>) {
    private val tokens: MutableList<String> = mutableListOf(*prefix, "!placeholder!")

    fun score(value: String): Double {
      tokens[tokens.lastIndex] = value
      return scoringFunction(tokens)
    }
  }
}