// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.completion.sorting.PrefixMatchingType
import com.intellij.psi.codeStyle.NameUtil

internal object PrefixMatchingUtil {
  const val baseName = "prefix_matching"

  fun calculateFeatures(element: LookupElement, scorer: PrefixMatchingScoringFunction, features: MutableMap<String, Any>) {
    if (scorer.prefix.isEmpty() || element.lookupString.isEmpty()) return
    val prefixMatchingScores = scorer.score(element.lookupString)
    features.addFeature("start_length", prefixMatchingScores.start(), 0)
    features.addFeature("symbols_length", prefixMatchingScores.symbols(), 0.0)
    features.addFeature("symbols_with_case_length", prefixMatchingScores.symbolsWithCase(), 0.0)
    features.addFeature("words_length", prefixMatchingScores.words(), 0.0)
    features.addFeature("words_with_case_length", prefixMatchingScores.wordsWithCase(), 0.0)
    features.addFeature("skipped_words", prefixMatchingScores.skippedWords(), 0)
    val wordsCount = prefixMatchingScores.wordsCount()
    if (wordsCount != 0) {
      features.addFeature("words_relative", prefixMatchingScores.words() / wordsCount, 0.0)
      features.addFeature("words_with_case_relative", prefixMatchingScores.wordsWithCase() / wordsCount, 0.0)
    }
    features.addFeature("type", prefixMatchingScores.type(scorer.prefix), PrefixMatchingType.UNKNOWN)
    features.addFeature("exact", prefixMatchingScores.exact(), false)
    features.addFeature("exact_final", prefixMatchingScores.exactFinal(), false)
  }

  fun createPrefixMatchingScoringFunction(prefix: String) = PrefixMatchingScoringFunction(prefix)

  class PrefixMatchingScoringFunction(val prefix: String) {
    fun score(value: String): PrefixMatchingScores {
      if (prefix.isEmpty()) return PrefixMatchingScores.EMPTY_PREFIX_MATCHING_SCORE
      val words = NameUtil.nameToWords(value)
      val scores = PrefixMatchingScores(value.commonPrefixWith(prefix, true).length,
                                        value == prefix,
                                        words.size,
                                        (words.lastOrNull() ?: "").length)
      val iter = prefix.iterator()
      var ch = iter.next()
      for ((i, word) in words.withIndex()) {
        if (!ch.isLetterOrDigit()) {
          if (value.contains(ch))
            ch = matchAndNext(scores, i, true, iter) ?: return scores
          else
            ch = next(iter) ?: return scores
        }
        for (wordCh in word) {
          if (wordCh.equals(ch, true))
            ch = matchAndNext(scores, i, wordCh == ch, iter) ?: return scores
          else break
        }
      }
      return scores
    }

    private fun matchAndNext(scores: PrefixMatchingScores, word: Int, withCase: Boolean, iter: Iterator<Char>): Char? {
      scores.updateMatching(word, withCase)
      return next(iter)
    }

    private fun next(iter: Iterator<Char>): Char? = if (iter.hasNext()) iter.next() else null
  }

  data class PrefixMatchingScores(private val start: Int,
                                  private val exact: Boolean,
                                  private val wordsCount: Int,
                                  private val lastWordSize: Int) {
    companion object {
      val EMPTY_PREFIX_MATCHING_SCORE = PrefixMatchingScores(0, false, 0, 0)
    }
    private var wordsMatchingMeasure = 0.0
    private var wordsMatchingWithCase = 0.0
    private var wordsMatchingCount = 0
    private var symbolsMatchingMeasure = 0.0
    private var symbolsMatchingCount = 0
    private var symbolsMatchingWithCaseMeasure = 0.0
    private var symbolsMatchingWithCaseCount = 0
    private var skippedWords = 0
    private var lastWord = 0
    private var curWord = -1

    fun updateMatching(word: Int, withCase: Boolean) {
      if (word != curWord) {
        val wordsDif = word - curWord
        skippedWords += wordsDif - 1
        val step = 1.0 / wordsDif
        curWord = word
        wordsMatchingMeasure += step
        symbolsMatchingMeasure += step
        wordsMatchingCount++
        if (withCase) {
          symbolsMatchingWithCaseMeasure += step
          wordsMatchingWithCase += step
        }
      } else {
        symbolsMatchingMeasure++
        if (withCase) symbolsMatchingWithCaseMeasure++
      }
      symbolsMatchingCount++
      if (withCase) symbolsMatchingWithCaseCount++
      if (word == wordsCount - 1) lastWord++
    }

    fun start(): Int = start
    fun exact(): Boolean = exact
    fun symbols(): Double = symbolsMatchingMeasure
    fun symbolsWithCase(): Double = symbolsMatchingWithCaseMeasure
    fun words(): Double = wordsMatchingMeasure
    fun wordsWithCase(): Double = wordsMatchingWithCase
    fun skippedWords(): Int = skippedWords
    fun exactFinal(): Boolean = lastWord == lastWordSize
    fun wordsCount(): Int = wordsCount
    fun type(prefix: String): PrefixMatchingType =
      when (prefix.length) {
        start -> PrefixMatchingType.START
        wordsMatchingCount -> PrefixMatchingType.FIRST_CHARS
        symbolsMatchingWithCaseCount -> PrefixMatchingType.SYMBOLS_WITH_CASE
        symbolsMatchingCount -> PrefixMatchingType.SYMBOLS
        else -> PrefixMatchingType.UNKNOWN
    }
  }

  private fun<T> MutableMap<String, T>.addFeature(name: String, value: T, defaultValue: T) {
    if (value != defaultValue) this["${baseName}_$name"] = value
  }
}