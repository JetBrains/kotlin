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
package com.intellij.completion.tracker

import com.intellij.codeInsight.completion.LightFixtureCompletionTestCase
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.impl.PrefixChangeListener
import org.assertj.core.api.Assertions


class PrefixChangeListenerTest: LightFixtureCompletionTestCase() {

  private val beforeChange = mutableListOf<LookupElement>()
  private val afterChange = mutableListOf<LookupElement>()
  private val lastLookupState = mutableListOf<LookupElement>()

  override fun setUp() {
    super.setUp()
    setupCompletionContext(myFixture)
  }

  fun `test prefix change listener`() {
    myFixture.completeBasic()

    lookup.setPrefixChangeListener(object : PrefixChangeListener {
      override fun afterAppend(c: Char) {
        afterChange.clear()
        afterChange.addAll(lookup.items)
      }

      override fun afterTruncate() {
        afterChange.clear()
        afterChange.addAll(lookup.items)
      }

      override fun beforeTruncate() {
        beforeChange.clear()
        beforeChange.addAll(lookup.items)
      }

      override fun beforeAppend(c: Char) {
        beforeChange.clear()
        beforeChange.addAll(lookup.items)
      }
    })

    lastLookupState.clear()
    lastLookupState.addAll(lookup.items)
    afterChange.clear()
    afterChange.addAll(lastLookupState)

    check { myFixture.type('r') }
    Assertions.assertThat(afterChange.size).isLessThan(beforeChange.size)

    check { myFixture.type('u') }
    Assertions.assertThat(afterChange.size).isLessThanOrEqualTo(beforeChange.size)

    check { myFixture.type('\b') }
    Assertions.assertThat(afterChange.size).isGreaterThanOrEqualTo(beforeChange.size)

    check { myFixture.type('\b') }
    Assertions.assertThat(afterChange.size).isGreaterThan(beforeChange.size)
  }

  private fun check(action: () -> Unit) {
    lastLookupState.clear()
    lastLookupState.addAll(lookup.items)

    Assertions.assertThat(afterChange).isEqualTo(lastLookupState)
    action()
    Assertions.assertThat(beforeChange).isEqualTo(lastLookupState)
  }


}