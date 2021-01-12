// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution

import junit.framework.TestCase.assertEquals
import org.jetbrains.plugins.gradle.execution.GradleRunnerUtil.parseComparisonMessage
import org.junit.Test

class GradleRunnerUtilTest {
  @Test
  fun `parse comparison message test`() {
    fun check(pattern: String, first: String, second: String) {
      parseComparisonMessage(pattern).let {
        assertEquals(first, it.first)
        assertEquals(second, it.second)
      }
    }

    check("expected:<[foo]> but was:<[Foo ]>", "[foo]", "[Foo ]")
    check("the problem ==> expected:<[foo]> but was:<[Foo ]>", "[foo]", "[Foo ]")

    check("expected: <foo> but was: <Foo >", "foo", "Foo ")
    check("the problem ==> expected: <foo> but was: <Foo >", "foo", "Foo ")

    check("expected same:<foo> was not:<Foo >", "foo", "Foo ")
    check("the problem ==> expected same:<foo> was not:<Foo >", "foo", "Foo ")
    check("the problem ==> expected: <foo> but was: <Foo >", "foo", "Foo ")
  }
}