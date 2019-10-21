// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.tracker

import com.intellij.codeInsight.completion.LightFixtureCompletionTestCase
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.stats.completion.LookupFinishListener
import junit.framework.TestCase

class LookupFinishTrackerTest : LightFixtureCompletionTestCase() {
  private val listener = TestFinishListener()
  override fun setUp() {
    super.setUp()
    setupCompletionContext(myFixture)
    myFixture.completeBasic()
    lookup.addLookupListener(listener)
  }

  fun `test cancelled`() {
    lookup.hide()
    assertFinishType(FinishType.CANCELLED)
  }

  fun `test explicit`() {
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
    assertFinishType(FinishType.EXPLICIT)
  }

  fun `test typed`() {
    myFixture.type('r')
    myFixture.type('u')
    myFixture.type('n')
    lookup.hide()
    assertFinishType(FinishType.TYPED)
  }

  private fun assertFinishType(type: FinishType) {
    TestCase.assertEquals(type, listener.finishType)
  }

  private class TestFinishListener : LookupFinishListener() {
    var finishType: FinishType = FinishType.UNKNOWN

    override fun cancelled(lookup: LookupImpl, canceledExplicitly: Boolean) = update(FinishType.CANCELLED)
    override fun explicitSelect(lookup: LookupImpl, element: LookupElement) = update(FinishType.EXPLICIT)
    override fun typedSelect(lookup: LookupImpl,
                             element: LookupElement) = update(FinishType.TYPED)

    private fun update(type: FinishType) {
      TestCase.assertEquals(finishType, FinishType.UNKNOWN)
      finishType = type
    }
  }

  private enum class FinishType {
    UNKNOWN, TYPED, CANCELLED, EXPLICIT
  }
}