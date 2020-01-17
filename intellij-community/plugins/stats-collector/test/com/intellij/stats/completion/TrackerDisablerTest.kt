// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.openapi.extensions.Extensions
import junit.framework.TestCase

class TrackerDisablerTest : CompletionLoggingTestBase() {
  fun `test disabler works`() = doTest(true)

  fun `test tracked without disabler`() = doTest(false)

  private fun doTest(shouldDisable: Boolean) {
    val disabler = registerDisabler(shouldDisable)
    myFixture.completeBasic()
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
    TestCase.assertTrue(disabler.checked)
    if (shouldDisable) {
      TestCase.assertTrue(trackedEvents.isEmpty())
    }
    else {
      TestCase.assertFalse(trackedEvents.isEmpty())
    }
  }

  private fun registerDisabler(disabled: Boolean): TestDisabler {
    val disabler = TestDisabler(disabled)
    Extensions.getRootArea().getExtensionPoint(CompletionTrackerDisabler.EpName).registerExtension(disabler, testRootDisposable)
    return disabler
  }

  private class TestDisabler(private val disabled: Boolean) : CompletionTrackerDisabler {
    var checked: Boolean = false
    override fun isDisabled(): Boolean {
      checked = true
      return disabled
    }
  }
}