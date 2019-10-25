// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.codeInsight.completion.LightFixtureCompletionTestCase
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.stats.completion.CompletionUtil
import org.assertj.core.api.Assertions.assertThat

class NgramPrefixExtractionTest : LightFixtureCompletionTestCase() {

  override fun setUp() {
    super.setUp()
    myFixture.addClass("""
interface XRunnable {
  void man();
  void run();
  void cat();
}
""")
  }

  fun `test ngram extraction`() {
    invokeCompletionWithText("class T { void r() { XRunnable x; x.<caret> } }")
    myFixture.completeBasic()
    checkExpectedNGram(".")
    checkExpectedNGram("x", ".")
    checkExpectedNGram("(", ")", "{", "XRunnable", "x", ";", "x", ".")
  }

  fun `test ngram extraction in file beginning`() {
    invokeCompletionWithText("<caret>class T { void r() { XRunnable x; } }")
    checkExpectedNGram()
  }

  fun `test ngram extraction in empty file`() {
    invokeCompletionWithText("<caret>")
    checkExpectedNGram()
  }

  private fun invokeCompletionWithText(javaFileText: String) {
    myFixture.configureByText(JavaFileType.INSTANCE, javaFileText)
    myFixture.completeBasic()
  }

  private fun checkExpectedNGram(vararg ngram: String) {
    val parameters = CompletionUtil.getCurrentCompletionParameters()
                     ?: return fail("Completion parameters not found. Session should be started")
    assertThat(NGram.getNGramPrefix(parameters, ngram.size + 1)).isEqualTo(ngram)
  }
}
