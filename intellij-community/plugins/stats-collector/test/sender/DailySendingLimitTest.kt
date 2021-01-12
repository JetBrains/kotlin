// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package sender

import com.intellij.stats.sender.DailyLimitSendingWatcher
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase

class DailySendingLimitTest : UsefulTestCase() {
  fun testLimitInfo() {
    val info = DailyLimitSendingWatcher.SentDataInfo.DumbInfo()
    TestCase.assertEquals(0, info.sentToday(10))
    info.dataSent(10, 100)
    TestCase.assertEquals(100, info.sentToday(10))
    info.dataSent(10, 1)
    TestCase.assertEquals(101, info.sentToday(10))
    val nextDay: Long = 10 + 24 * 60 * 60 * 1000
    TestCase.assertEquals(0, info.sentToday(nextDay))
    info.dataSent(nextDay, 1)
    TestCase.assertEquals(1, info.sentToday(nextDay))
  }

  fun testSendingWatcher() {
    val watcher = DailyLimitSendingWatcher(2500, DailyLimitSendingWatcher.SentDataInfo.DumbInfo())
    TestCase.assertFalse(watcher.isLimitReached())
    watcher.dataSent(2300)
    TestCase.assertFalse(watcher.isLimitReached())
    watcher.dataSent(500)
    if (!watcher.isLimitReached()) {
      println("is it really 12 o'clock?!")
      watcher.dataSent(2300)
      watcher.dataSent(500)
    }

    TestCase.assertTrue(watcher.isLimitReached())
  }
}