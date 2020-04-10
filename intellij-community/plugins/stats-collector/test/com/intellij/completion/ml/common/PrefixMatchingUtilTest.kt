// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

import org.assertj.core.api.Assertions
import org.junit.Test

class PrefixMatchingUtilTest {
  private val camelCaseLookupString = "isEmptyString"
  private val capitalizedLookupString = "IS_EMPTY_STRING"
  private val snakeCaseLookupString = "is_empty_string"

  @Test
  fun testStartPrefixMatching() {
    val prefix = "isempt"
    val checks = listOf(
      ScoreCheck(6, PrefixMatchingUtil.PrefixMatchingScores::start),
      ScoreCheck(6.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(5.0, PrefixMatchingUtil.PrefixMatchingScores::symbolsWithCase),
      ScoreCheck(2.0, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(1.0, PrefixMatchingUtil.PrefixMatchingScores::wordsWithCase),
      ScoreCheck(PrefixMatchingType.START, PrefixMatchingUtil.PrefixMatchingScores::type))

    checkScores(prefix, camelCaseLookupString, checks)
  }

  @Test
  fun testStartPrefixWithCaseMatching() {
    val prefix = "isEmpt"
    val checks = listOf(
      ScoreCheck(6, PrefixMatchingUtil.PrefixMatchingScores::start),
      ScoreCheck(6.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(6.0, PrefixMatchingUtil.PrefixMatchingScores::symbolsWithCase),
      ScoreCheck(2.0, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(2.0, PrefixMatchingUtil.PrefixMatchingScores::wordsWithCase),
      ScoreCheck(PrefixMatchingType.START, PrefixMatchingUtil.PrefixMatchingScores::type))

    checkScores(prefix, camelCaseLookupString, checks)
  }

  @Test
  fun testFirstCharsMatching() {
    val prefix = "ies"
    val checks = listOf(
      ScoreCheck(1, PrefixMatchingUtil.PrefixMatchingScores::start),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(PrefixMatchingType.FIRST_CHARS, PrefixMatchingUtil.PrefixMatchingScores::type))

    checkScores(prefix, camelCaseLookupString, checks)
    checkScores(prefix, capitalizedLookupString, checks)
    checkScores(prefix, snakeCaseLookupString, checks)
  }

  @Test
  fun testFirstCharsWithCaseMatching() {
    val prefix = "iES"
    val checks = listOf(
      ScoreCheck(1, PrefixMatchingUtil.PrefixMatchingScores::start),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::symbolsWithCase),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::wordsWithCase),
      ScoreCheck(PrefixMatchingType.FIRST_CHARS, PrefixMatchingUtil.PrefixMatchingScores::type))

    checkScores(prefix, camelCaseLookupString, checks)
  }

  @Test
  fun testSymbolsWithCaseMatching() {
    val prefix = "isEmpSt"
    val checks = listOf(
      ScoreCheck(5, PrefixMatchingUtil.PrefixMatchingScores::start),
      ScoreCheck(7.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(7.0, PrefixMatchingUtil.PrefixMatchingScores::symbolsWithCase),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::wordsWithCase),
      ScoreCheck(PrefixMatchingType.SYMBOLS_WITH_CASE, PrefixMatchingUtil.PrefixMatchingScores::type))

    checkScores(prefix, camelCaseLookupString, checks)
  }

  @Test
  fun testSymbolsMatching() {
    val prefix = "isempst"
    val checks = listOf(
      ScoreCheck(5, PrefixMatchingUtil.PrefixMatchingScores::start),
      ScoreCheck(7.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(5.0, PrefixMatchingUtil.PrefixMatchingScores::symbolsWithCase),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(1.0, PrefixMatchingUtil.PrefixMatchingScores::wordsWithCase),
      ScoreCheck(PrefixMatchingType.SYMBOLS, PrefixMatchingUtil.PrefixMatchingScores::type))

    checkScores(prefix, camelCaseLookupString, checks)
  }

  @Test
  fun testSymbolsMatchingConsistency() {
    val prefix = "isempst"
    val checks = listOf(
      ScoreCheck(7.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::words))

    checkScores(prefix, camelCaseLookupString, checks)
    checkScores(prefix, capitalizedLookupString, checks)
    checkScores(prefix, snakeCaseLookupString, checks)
  }

  @Test
  fun testSkippedWords() {
    val prefix = "isstr"
    val checks = listOf(
      ScoreCheck(4.5, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(1.5, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(1, PrefixMatchingUtil.PrefixMatchingScores::skippedWords))

    checkScores(prefix, camelCaseLookupString, checks)
    checkScores(prefix, capitalizedLookupString, checks)
    checkScores(prefix, snakeCaseLookupString, checks)
  }


  @Test
  fun testFinalMatching() {
    val prefix = "iemstring"
    val checks = listOf(
      ScoreCheck(9.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(3.0, PrefixMatchingUtil.PrefixMatchingScores::words),
      ScoreCheck(true, PrefixMatchingUtil.PrefixMatchingScores::exactFinal))

    checkScores(prefix, camelCaseLookupString, checks)
    checkScores(prefix, capitalizedLookupString, checks)
    checkScores(prefix, snakeCaseLookupString, checks)
  }

  @Test
  fun testExactMatching() {
    val prefix = "isEmptyString"
    checkScores(prefix, camelCaseLookupString, listOf(
      ScoreCheck(true, PrefixMatchingUtil.PrefixMatchingScores::exact)))
  }

  @Test
  fun testEmptyPrefix() {
    val prefix = ""
    val checks = listOf(
      ScoreCheck(0, PrefixMatchingUtil.PrefixMatchingScores::start),
      ScoreCheck(0.0, PrefixMatchingUtil.PrefixMatchingScores::symbols),
      ScoreCheck(0.0, PrefixMatchingUtil.PrefixMatchingScores::words))

    checkScores(prefix, camelCaseLookupString, checks)
    checkScores(prefix, capitalizedLookupString, checks)
    checkScores(prefix, snakeCaseLookupString, checks)
  }

  private data class ScoreCheck<out T>(val expected: T, val accessor: (PrefixMatchingUtil.PrefixMatchingScores) -> T)

  private fun<T> checkScores(prefix: String, lookupString: String, checks: List<ScoreCheck<T>>) {
    val actual = PrefixMatchingUtil.PrefixMatchingScores.Builder().build(prefix, lookupString)
    for (check in checks)
      Assertions.assertThat(check.accessor(actual)).isEqualTo(check.expected)
  }
}