// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.codeStyle.NameUtil

object PrefixMatchingUtil {
  const val baseName = "prefix"

  fun calculateFeatures(element: LookupElement, prefix: String, features: MutableMap<String, Any>) {
    if (prefix.isEmpty() || element.lookupString.isEmpty()) return
    val prefixMatchingScores = PrefixMatchingScores.Builder().build(prefix, element.lookupString)
    // how many chars of the prefix are matched with the beginning of the lookup string
    features.addFeature("same_start_count", prefixMatchingScores.start, 0)
    // greedy matcher tries to match chars of the prefix with a word in the lookup string
    // when chars are not matched, it goes on to the next word
    // if it skips words, the score is increased not by 1, but 1/(skipped words count + 1)
    features.addFeature("greedy_score", prefixMatchingScores.greedy, 0.0)
    // case sensitive version of the previous feature
    features.addFeature("greedy_with_case_score", prefixMatchingScores.greedyWithCase, 0.0)
    // number of words matched by greedy matcher; score is calculated in the same way
    features.addFeature("matched_words_score", prefixMatchingScores.words, 0.0)
    // value of the previous feature in relation to words count in the lookup string
    features.addFeature("matched_words_relative", prefixMatchingScores.wordsRelative, 0.0)
    // case sensitive version of the matched_words_score feature
    features.addFeature("matched_words_with_case_score", prefixMatchingScores.wordsWithCase, 0.0)
    // case sensitive version of matched_words_relative feature
    features.addFeature("matched_words_with_case_relative", prefixMatchingScores.wordsWithCaseRelative, 0.0)
    // number of skipped words by the greedy matcher
    features.addFeature("skipped_words", prefixMatchingScores.skippedWords, 0)
    // the most suitable matching type for prefix and lookup string
    features.addFeature("matching_type", prefixMatchingScores.type, PrefixMatchingType.UNKNOWN)
    // are the prefix and lookup string the same
    features.addFeature("exact", prefixMatchingScores.exact, false)
    // did the last word of the lookup string match the end of the prefix
    features.addFeature("matched_last_word", prefixMatchingScores.exactFinal, false)
  }

  data class PrefixMatchingScores internal constructor(
    val exact: Boolean,
    val exactFinal: Boolean,
    val start: Int,
    val greedy: Double,
    val greedyWithCase: Double,
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
      private var wordsMatchingScore = 0.0
      private var wordsMatchingWithCaseScore = 0.0
      private var wordsMatchingCount = 0
      private var greedyMatchingScore = 0.0
      private var greedyMatchingCount = 0
      private var greedyMatchingWithCaseScore = 0.0
      private var greedyMatchingWithCaseCount = 0
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
          greedyMatchingScore,
          greedyMatchingWithCaseScore,
          wordsMatchingScore,
          if (wordsCount == 0) 0.0 else wordsMatchingScore / wordsCount,
          wordsMatchingWithCaseScore,
          if (wordsCount == 0) 0.0 else wordsMatchingWithCaseScore / wordsCount,
          skippedWords,
          resolveMatchingType(prefix)
        )
      }

      private fun resolveMatchingType(prefix: String): PrefixMatchingType =
        when (prefix.length) {
          startMatchingCount -> PrefixMatchingType.START_WITH
          wordsMatchingCount -> PrefixMatchingType.WORDS_FIRST_CHAR
          greedyMatchingWithCaseCount -> PrefixMatchingType.GREEDY_WITH_CASE
          greedyMatchingCount -> PrefixMatchingType.GREEDY
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
          wordsMatchingScore += step
          greedyMatchingScore += step
          wordsMatchingCount++
          if (withCase) {
            greedyMatchingWithCaseScore += step
            wordsMatchingWithCaseScore += step
          }
        } else {
          greedyMatchingScore++
          if (withCase) greedyMatchingWithCaseScore++
        }
        greedyMatchingCount++
        if (withCase) greedyMatchingWithCaseCount++
        if (word == wordsCount - 1) lastWord++
      }
    }
  }

  private fun<T> MutableMap<String, T>.addFeature(name: String, value: T, defaultValue: T) {
    if (value != defaultValue) this["${baseName}_$name"] = value
  }
}