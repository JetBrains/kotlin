// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.completion.ml.common.LocationFeaturesUtil
import org.junit.Assert
import org.junit.Test

class IndentLevelTest {
  @Test
  fun emptyLine() {
    checkLine(0, "", 1)
    checkLine(0, "", 2)
  }

  @Test
  fun noIndent() {
    checkLine(0, "a", 1)
    checkLine(0, " a", 2)
    checkLine(0, "  a", 4)
  }

  @Test
  fun spaces() {
    checkLine(1, "  val a = 100", 2)
    checkLine(2, "    val a = 100", 2)
    checkLine(1, "    val a = 100", 4)
  }

  @Test
  fun tabs() {
    checkLine(1, "\tval a = 100", 4)
    checkLine(2, "\t\tval a = 100", 10)
  }

  @Test
  fun mixed() {
    checkLine(2, " \t\tval a = 100", 2)
    checkLine(2, " \t\tval a = 100", 3)
    checkLine(2, " \t\t val a = 100", 3)
    checkLine(3, " \t\t   val a = 100", 3)
  }

  private fun checkLine(expectedIndent: Int, linePrefix: String, tabSize: Int) {
    Assert.assertEquals(expectedIndent, LocationFeaturesUtil.indentLevel(linePrefix, tabSize))
  }
}