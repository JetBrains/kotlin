// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.codeStyle.NameUtil

object PrefixMatchingUtil {
  const val baseName = "prefix_matching"

  fun calculateFeatures(element: LookupElement, prefix: String, features: MutableMap<String, Any>) {
    if (prefix.isEmpty() || element.lookupString.isEmpty()) return
    val prefixMatchingScores = PrefixMatchingScores.Builder().build(prefix, element.lookupString)
    features.addFeature("start_length", prefixMatchingScores.start, 0)
    features.addFeature("symbols_length", prefixMatchingScores.symbols, 0.0)
    features.addFeature("symbols_with_case_length", prefixMatchingScores.symbolsWithCase, 0.0)
    features.addFeature("words_length", prefixMatchingScores.words, 0.0)
    features.addFeature("words_relative", prefixMatchingScores.wordsRelative, 0.0)
    features.addFeature("words_with_case_length", prefixMatchingScores.wordsWithCase, 0.0)
    features.addFeature("words_with_case_relative", prefixMatchingScores.wordsWithCaseRelative, 0.0)
    features.addFeature("skipped_words", prefixMatchingScores.skippedWords, 0)
    features.addFeature("type", prefixMatchingScores.type, PrefixMatchingType.UNKNOWN)
    features.addFeature("exact", prefixMatchingScores.exact, false)
    features.addFeature("exact_final", prefixMatchingScores.exactFinal, false)
  }

  data class PrefixMatchingScores internal constructor(
      val exact: Boolean,
      val exactFinal: Boolean,
      val start: Int,
      val symbols: Double,
      val symbolsWithCase: Double,
      val words: Double,
      val wordsRelative: Double,
      val wordsWithCase: Double,
      val wordsWithCaseRelative: Double,
      val skippedWords: Int,
      val type: PrefixMatchingType) {

    companion object {
      private val EMPTY_PREFIX_MATCHING_SCORE =
        PrefixMatchingScores(false, false, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, PrefixMatchingType.UNKNOWN)
    }

    class Builder {
      private var startMatchingCount = 0
      private var exact = false
      private var wordsMatchingMeasure = 0.0
      private var wordsMatchingWithCaseMeasure = 0.0
      private var wordsMatchingCount = 0
      private var symbolsMatchingMeasure = 0.0
      private var symbolsMatchingCount = 0
      private var symbolsMatchingWithCaseMeasure = 0.0
      private var symbolsMatchingWithCaseCount = 0
      private var skippedWords = 0
      private var lastWord = 0
      private var lastWordSize = 0
      private var wordsCount = 0
      private var curWord = -1

      fun build(prefix: String, lookupString: String): PrefixMatchingScores {
        if (prefix.isEmpty()) return EMPTY_PREFIX_MATCHING_SCORE
        val words = NameUtil.nameToWords(lookupString).filter { it.all { it.isLetterOrDigit() } }
        startMatchingCount = lookupString.commonPrefixWith(prefix, true).length
        exact = lookupString == prefix
        wordsCount = words.size
        lastWordSize = (words.lastOrNull() ?: "").length

        val iter = prefix.iterator()
        var ch = iter.next()
        for ((i, word) in words.withIndex()) {
          if (!ch.isLetterOrDigit()) {
            if (lookupString.contains(ch))
              ch = matchAndNext(i, true, iter) ?: break
            else
              ch = next(iter) ?: break
          }
          ch = matchWord(word, ch, i, iter) ?: break
        }

        return PrefixMatchingScores(
          exact,
          lastWord == lastWordSize,
          startMatchingCount,
          symbolsMatchingMeasure,
          symbolsMatchingWithCaseMeasure,
          wordsMatchingMeasure,
          if (wordsCount == 0) 0.0 else wordsMatchingMeasure / wordsCount,
          wordsMatchingWithCaseMeasure,
          if (wordsCount == 0) 0.0 else wordsMatchingWithCaseMeasure / wordsCount,
          skippedWords,
          resolveMatchingType(prefix)
        )
      }

      private fun resolveMatchingType(prefix: String): PrefixMatchingType =
        when (prefix.length) {
          startMatchingCount -> PrefixMatchingType.START
          wordsMatchingCount -> PrefixMatchingType.FIRST_CHARS
          symbolsMatchingWithCaseCount -> PrefixMatchingType.SYMBOLS_WITH_CASE
          symbolsMatchingCount -> PrefixMatchingType.SYMBOLS
          else -> PrefixMatchingType.UNKNOWN
        }

      private fun matchWord(word: String, ch: Char, wordIndex: Int, iter: Iterator<Char>): Char? {
        var curCh = ch
        for (wordCh in word) {
          if (wordCh.equals(curCh, true))
            curCh = matchAndNext(wordIndex, wordCh == curCh, iter) ?: return null
          else break
        }
        return curCh
      }

      private fun matchAndNext(wordIndex: Int, withCase: Boolean, iter: Iterator<Char>): Char? {
        updateMatching(wordIndex, withCase)
        return next(iter)
      }

      private fun next(iter: Iterator<Char>): Char? = if (iter.hasNext()) iter.next() else null

      private fun updateMatching(word: Int, withCase: Boolean) {
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
            wordsMatchingWithCaseMeasure += step
          }
        } else {
          symbolsMatchingMeasure++
          if (withCase) symbolsMatchingWithCaseMeasure++
        }
        symbolsMatchingCount++
        if (withCase) symbolsMatchingWithCaseCount++
        if (word == wordsCount - 1) lastWord++
      }
    }
  }

  private fun<T> MutableMap<String, T>.addFeature(name: String, value: T, defaultValue: T) {
    if (value != defaultValue) this["${baseName}_$name"] = value
  }
}