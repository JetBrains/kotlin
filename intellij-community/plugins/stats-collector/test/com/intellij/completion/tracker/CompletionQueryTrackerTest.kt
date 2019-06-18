// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.tracker

import com.intellij.stats.personalization.session.CompletionQueryTrackerImpl
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase

class CompletionQueryTrackerTest : UsefulTestCase() {
  private val tracker = CompletionQueryTrackerImpl(System.currentTimeMillis())
  fun `test no queries performed`() {
    checkTrackerStatus(1, 1, 1)
  }

  fun `test one query performed`() {
    tracker.afterAppend('x')
    checkTrackerStatus(2, 2, 1)
  }

  fun `test two distinct queries`() {
    tracker.afterAppend('f')
    tracker.afterAppend('o')
    checkTrackerStatus(3, 3, 1)
  }

  fun `test append and remove char`() {
    tracker.afterAppend('f')
    tracker.afterTruncate()
    checkTrackerStatus(2, 3, 2)
  }

  fun `test append twice and remove`() {
    tracker.afterAppend('f')
    tracker.afterAppend('o')
    tracker.afterTruncate()
    checkTrackerStatus(3, 4, 2)
  }

  fun `test single truncate`() {
    tracker.afterTruncate()
    checkTrackerStatus(2, 2, 1)
  }

  fun `test one append two truncate`() {
    tracker.afterAppend('f')
    tracker.afterTruncate()
    tracker.afterTruncate()
    checkTrackerStatus(3, 4, 1)
  }

  fun `test append after truncate`() {
    tracker.afterTruncate()
    tracker.afterAppend('f')
    checkTrackerStatus(3, 3, 1)
  }

  fun `test append the same after truncate`() {
    tracker.afterTruncate()
    tracker.afterAppend('f')
    tracker.afterTruncate()
    tracker.afterAppend('f')
    checkTrackerStatus(3, 5, 2)
  }

  private fun checkTrackerStatus(uniqueQueries: Int, totalQueries: Int, currentQueryFrequency: Int) {
    TestCase.assertEquals(uniqueQueries, tracker.getUniqueQueriesCount())
    TestCase.assertEquals(totalQueries, tracker.getTotalQueriesCount())
    TestCase.assertEquals(currentQueryFrequency, tracker.getCurrentQueryFrequency())
  }
}