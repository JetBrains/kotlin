// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

import org.assertj.core.api.Assertions
import org.junit.Test

class PrefixMatchingUtilTest {
  private val lookupString = "isEmptyString"

  @Test
  fun testStartPrefixMatching() {
    val prefix = "isempt"
    checkScores(prefix, listOf(
      ScoreCheck(6, PrefixMatchingUtil.PrefixMatchingScores::start),
      ScoreCheck(6.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(5.0, PrefixMatchingUtil.PrefixMatchingScores::symbolsWithCase),
      ScoreCheck(2.0, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(1.0, PrefixMatchingUtil.PrefixMatchingScores::wordsWithCase),
      ScoreCheck(PrefixMatchingUtil.PrefixMatchingType.START, PrefixMatchingUtil.PrefixMatchingScores::type)))
  }

  @Test
  fun testStartPrefixWithCaseMatching() {
    val prefix = "isEmpt"
    checkScores(prefix, listOf(
      ScoreCheck(6, PrefixMatchingUtil.PrefixMatchingScores::start),
      ScoreCheck(6.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(6.0, PrefixMatchingUtil.PrefixMatchingScores::symbolsWithCase),
      ScoreCheck(2.0, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(2.0, PrefixMatchingUtil.PrefixMatchingScores::wordsWithCase),
      ScoreCheck(PrefixMatchingUtil.PrefixMatchingType.START, PrefixMatchingUtil.PrefixMatchingScores::type)))
  }

  @Test
  fun testFirstCharsMatching() {
    val prefix = "ies"
    checkScores(prefix, listOf(
      ScoreCheck(1, PrefixMatchingUtil.PrefixMatchingScores::start),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(1.0, PrefixMatchingUtil.PrefixMatchingScores::symbolsWithCase),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(1.0, PrefixMatchingUtil.PrefixMatchingScores::wordsWithCase),
      ScoreCheck(PrefixMatchingUtil.PrefixMatchingType.FIRST_CHARS, PrefixMatchingUtil.PrefixMatchingScores::type)))
  }

  @Test
  fun testFirstCharsWithCaseMatching() {
    val prefix = "iES"
    checkScores(prefix, listOf(
      ScoreCheck(1, PrefixMatchingUtil.PrefixMatchingScores::start),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::symbolsWithCase),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::wordsWithCase),
      ScoreCheck(PrefixMatchingUtil.PrefixMatchingType.FIRST_CHARS, PrefixMatchingUtil.PrefixMatchingScores::type)))
  }

  @Test
  fun testSymbolsWithCaseMatching() {
    val prefix = "isEmpSt"
    checkScores(prefix, listOf(
      ScoreCheck(5, PrefixMatchingUtil.PrefixMatchingScores::start),
      ScoreCheck(7.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(7.0, PrefixMatchingUtil.PrefixMatchingScores::symbolsWithCase),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::wordsWithCase),
      ScoreCheck(PrefixMatchingUtil.PrefixMatchingType.SYMBOLS_WITH_CASE, PrefixMatchingUtil.PrefixMatchingScores::type)))
  }

  @Test
  fun testSymbolsMatching() {
    val prefix = "isempst"
    checkScores(prefix, listOf(
      ScoreCheck(5, PrefixMatchingUtil.PrefixMatchingScores::start),
      ScoreCheck(7.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(5.0, PrefixMatchingUtil.PrefixMatchingScores::symbolsWithCase),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(1.0, PrefixMatchingUtil.PrefixMatchingScores::wordsWithCase),
      ScoreCheck(PrefixMatchingUtil.PrefixMatchingType.SYMBOLS, PrefixMatchingUtil.PrefixMatchingScores::type)))
  }

  @Test
  fun testSkippedWords() {
    val prefix = "isstr"
    checkScores(prefix, listOf(
      ScoreCheck(4.5, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(1.5, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(1, PrefixMatchingUtil.PrefixMatchingScores::skippedWords),
      ScoreCheck(PrefixMatchingUtil.PrefixMatchingType.SYMBOLS, PrefixMatchingUtil.PrefixMatchingScores::type)))
  }


  @Test
  fun testFinalMatching() {
    val prefix = "iemstring"
    checkScores(prefix, listOf(
      ScoreCheck(9.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(true, PrefixMatchingUtil.PrefixMatchingScores::exactFinal),
      ScoreCheck(PrefixMatchingUtil.PrefixMatchingType.SYMBOLS, PrefixMatchingUtil.PrefixMatchingScores::type)))
  }

  @Test
  fun testExactMatching() {
    val prefix = "isEmptyString"
    checkScores(prefix, listOf(
      ScoreCheck(true, PrefixMatchingUtil.PrefixMatchingScores::exact)))
  }

  private data class ScoreCheck<out T>(val expected: T, val accessor: (PrefixMatchingUtil.PrefixMatchingScores) -> T)

  private fun<T> checkScores(prefix: String, checks: List<ScoreCheck<T>>) {
    val actual = PrefixMatchingUtil.PrefixMatchingScores.Builder().build(prefix, lookupString)
    for (check in checks)
      Assertions.assertThat(check.accessor(actual)).isEqualTo(check.expected)
  }
}