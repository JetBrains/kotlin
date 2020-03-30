// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import org.assertj.core.api.Assertions

class CompletionLoggingQueryLengthTest: CompletionLoggingTestBase() {
  override fun setUp() {
    super.setUp()
    myFixture.addClass("interface Rum {}")
    myFixture.addClass("interface Runn {}")
  }

  fun `test completion with query length 1 after dot`() {
    myFixture.type('.')
    myFixture.completeBasic()

    val prefixLength = lookup.queryLength()

    Assertions.assertThat(prefixLength).isEqualTo(1)
  }

  fun `test completion with query length 3 after dot`() {
    myFixture.type(".r")
    myFixture.completeBasic()
    myFixture.type("u")
    myFixture.type("n")

    val prefixLength = lookup.queryLength()

    Assertions.assertThat(prefixLength).isEqualTo(3)
  }


  fun `test completion with query length 1`() {
    myFixture.type('\b')
    myFixture.type("Run")
    myFixture.completeBasic()

    val prefixLength = lookup.queryLength()

    Assertions.assertThat(prefixLength).isEqualTo(1)
  }


  fun `test completion with query length 2`() {
    myFixture.type('\b')
    myFixture.type('R')
    myFixture.completeBasic()
    myFixture.type('u')

    val prefixLength = lookup.queryLength()

    Assertions.assertThat(prefixLength).isEqualTo(2)
  }
}