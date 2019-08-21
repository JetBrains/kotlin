// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.codeInsight.completion.LightFixtureCompletionTestCase
import com.intellij.completion.ngram.Ngram.Companion.getNgramPrefix
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
    myFixture.configureByText(JavaFileType.INSTANCE, "class T { void r() { XRunnable x; x.<caret> } }")
  }

  fun `test ngram extraction`() {
    myFixture.completeBasic()
    val parameters = CompletionUtil.getCurrentCompletionParameters() ?: return
    val bigramPrefix = getNgramPrefix(parameters, 2)
    val trigramPrefix = getNgramPrefix(parameters, 3)
    val nonagramPrefix = getNgramPrefix(parameters, 9)
    assertThat(bigramPrefix).isEqualTo(arrayOf("."))
    assertThat(trigramPrefix).isEqualTo(arrayOf("x", "."))
    assertThat(nonagramPrefix).isEqualTo(arrayOf("(", ")", "{", "XRunnable", "x", ";", "x", "."))
  }
}
