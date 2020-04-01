// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.codeStyle.NameUtil
import com.intellij.stats.personalization.impl.PrefixMatchingType

internal object PrefixMatchingUtil {
  private const val baseName = "prefix_matching"

  fun calculateFeatures(element: LookupElement, scorer: PrefixMatchingScoringFunction, features: MutableMap<String, Any>) {
    if (scorer.prefix.isEmpty() || element.lookupString.isEmpty()) return
    val wordsCount = NameUtil.nameToWords(element.lookupString).size
    val prefixMatchingScores = scorer.score(element.lookupString)
    features.addFeature("start_length", prefixMatchingScores.start(), 0)
    features.addFeature("symbols_length", prefixMatchingScores.symbols(), 0.0)
    features.addFeature("symbols_with_case_length", prefixMatchingScores.symbolsWithCase(), 0.0)
    features.addFeature("words_length", prefixMatchingScores.words(), 0.0)
    features.addFeature("words_relative", prefixMatchingScores.words() / wordsCount, 0.0)
    features.addFeature("words_with_case_length", prefixMatchingScores.wordsWithCase(), 0.0)
    features.addFeature("words_with_case_relative", prefixMatchingScores.wordsWithCase() / wordsCount, 0.0)
    features.addFeature("type", prefixMatchingScores.type(scorer.prefix), PrefixMatchingType.UNDEFINED)
    features.addFeature("exact", prefixMatchingScores.exact(), false)
  }

  fun createPrefixMatchingScoringFunction(prefix: String) = PrefixMatchingScoringFunction(prefix)

  class PrefixMatchingScoringFunction(val prefix: String) {
    fun score(value: String): PrefixMatchingScores {
      val scores = PrefixMatchingScores(value.commonPrefixWith(prefix, true).length, value == prefix)
      if (prefix.isEmpty()) return scores
      val words = NameUtil.nameToWords(value)
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

  class PrefixMatchingScores(private val start: Int, private val exact: Boolean) {
    private var words = 0.0
    private var symbolsMeasure = 0.0
    private var symbolsCount = 0
    private var symbolsWithCaseMeasure = 0.0
    private var symbolsWithCaseCount = 0
    private var wordsWithCase = 0.0
    private var curWord = -1

    fun updateMatching(word: Int, withCase: Boolean) {
      if (word != curWord) {
        val step = 1.0 / (word - curWord)
        curWord = word
        words += step
        symbolsMeasure += step
        if (withCase) {
          symbolsWithCaseMeasure += step
          wordsWithCase += step
        }
      } else {
        symbolsMeasure++
        if (withCase) symbolsWithCaseMeasure++
      }
      symbolsCount++
      if (withCase) symbolsWithCaseCount++
    }

    fun start(): Int = start
    fun exact(): Boolean = exact
    fun symbols(): Double = symbolsMeasure
    fun symbolsWithCase(): Double = symbolsWithCaseMeasure
    fun words(): Double = words
    fun wordsWithCase(): Double = wordsWithCase
    fun type(prefix: String): PrefixMatchingType =
      when (prefix.length) {
        start -> PrefixMatchingType.START
        symbolsWithCaseCount -> PrefixMatchingType.SYMBOLS_WITH_CASE
        symbolsCount -> PrefixMatchingType.SYMBOLS
        else -> PrefixMatchingType.UNDEFINED
    }
  }

  private fun MutableMap<String, Any>.addFeature(name: String, value: Any, defaultValue: Any) {
    if (value != defaultValue) this["${baseName}_$name"] = value
  }
}