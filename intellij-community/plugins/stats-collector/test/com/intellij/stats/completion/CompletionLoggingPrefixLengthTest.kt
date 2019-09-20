/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.stats.completion

import org.assertj.core.api.Assertions.assertThat


class CompletionLoggingPrefixLengthTest: CompletionLoggingTestBase() {


    override fun setUp() {
        super.setUp()
        myFixture.addClass("interface Rum {}")
        myFixture.addClass("interface Runn {}")
    }

    fun `test completion on dot starts has prefix length 1`() {
    myFixture.type('.')
    myFixture.completeBasic()

    val prefixLength = lookup.prefixLength()

    myFixture.type("ru\n")
    assertThat(prefixLength).isEqualTo(1)
  }

  fun `test prefix length 1`() {
    myFixture.type(".ru")
    myFixture.completeBasic()

    val prefixLength = lookup.prefixLength()

    assertThat(prefixLength).isEqualTo(1)
  }


    fun `test if completion starts with 3 chars prefix is still 1`() {
        myFixture.type('\b')
        myFixture.type("Run")
        myFixture.completeBasic()

        val prefixLength = lookup.prefixLength()

        myFixture.type('\n')

        assertThat(prefixLength).isEqualTo(1)
    }


  fun `test if completion starts with 1 char prefix is 1`() {
    myFixture.type('\b')
    myFixture.type('R')
    myFixture.completeBasic()

    val prefixLength = lookup.prefixLength()

    myFixture.type("un\n")

    assertThat(prefixLength).isEqualTo(1)
  }


}