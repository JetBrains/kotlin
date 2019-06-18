// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.tracker

import com.intellij.stats.personalization.session.PeriodTracker
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase

class PeriodTrackerTest : UsefulTestCase() {
  private val tracker = PeriodTracker()
  fun `test consistent when empty`() {
    TestCase.assertEquals(0L, tracker.totalTime(null))
    TestCase.assertEquals(0, tracker.count(null))
    TestCase.assertEquals(0.0, tracker.average(null))
    TestCase.assertNull(tracker.minDuration(null))
    TestCase.assertNull(tracker.maxDuration(null))
  }

  fun `test empty with while current period is in process`() {
    TestCase.assertEquals(42L, tracker.totalTime(42))
    TestCase.assertEquals(1, tracker.count(100))
    TestCase.assertEquals(15.0, tracker.average(15))
    TestCase.assertEquals(100L, tracker.minDuration(100))
    TestCase.assertEquals(200L, tracker.maxDuration(200))
  }

  fun `test consistent if past observations exist`() {
    tracker.addDuration(100)
    TestCase.assertEquals(100L, tracker.totalTime(null))
    TestCase.assertEquals(142L, tracker.totalTime(42))

    TestCase.assertEquals(1, tracker.count(null))
    TestCase.assertEquals(2, tracker.count(100))

    TestCase.assertEquals(100.0, tracker.average(null))
    TestCase.assertEquals(70.0, tracker.average(40))

    TestCase.assertEquals(100L, tracker.minDuration(null))
    TestCase.assertEquals(100L, tracker.minDuration(150))
    TestCase.assertEquals(30L, tracker.minDuration(30))

    TestCase.assertEquals(100L, tracker.maxDuration(null))
    TestCase.assertEquals(100L, tracker.maxDuration(30))
    TestCase.assertEquals(200L, tracker.maxDuration(200))
  }
}
