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
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.stats.completion.idString
import com.intellij.stats.completion.ElementPositionHistory
import com.intellij.stats.completion.StagePosition
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import org.assertj.core.api.Assertions.assertThat


class TimesShownTrackingTest : LightFixtureCompletionTestCase() {

  override fun setUp() {
    super.setUp()
    setupCompletionContext(myFixture)
  }

  fun `test check times shown`() {
    myFixture.completeBasic()
    val allItems = lookup.items

    val shownTimesTracker = PositionTrackingListener(lookup)
    lookup.setPrefixChangeListener(shownTimesTracker)

    assertThat(allItems.take(5).map { it.lookupString })
      .isEqualTo(listOf("cat", "man", "run", "runnable", "rus"))

    myFixture.type("r")
    val history: MutableMap<String, ElementPositionHistory> = UserDataLookupElementPositionTracker.history(lookup)!!
    myFixture.type("us\n")

    val map = allItems.map {
      val id = it.idString()
      it.lookupString to history[id]?.history()
    }.toMap()

    assertThat(map["man"]).isEqualTo(listOf(
            StagePosition(0, 1)
    ))

    assertThat(map["cat"]).isEqualTo(listOf(
            StagePosition(0, 0)
    ))

    assertThat(map["run"]).isEqualTo(listOf(
            StagePosition(0, 2),
            StagePosition(1, 0),
            StagePosition(2, 0)
    ))

    assertThat(map["runnable"]).isEqualTo(listOf(
            StagePosition(0, 3),
            StagePosition(1, 1),
            StagePosition(2, 1)
    ))

    assertThat(map["rus"]).isEqualTo(listOf(
            StagePosition(0, 4),
            StagePosition(1, 2),
            StagePosition(2, 2)
    ))
  }

}


internal fun setupCompletionContext(fixture: JavaCodeInsightTestFixture) {
  fixture.addClass("""
interface XRunnable {
  void man();
  void run();
  void cat();
  void runnable();
  void rus();
}
""")

  fixture.configureByText(JavaFileType.INSTANCE, "class T { void r() { XRunnable x; x.<caret> } }")
}