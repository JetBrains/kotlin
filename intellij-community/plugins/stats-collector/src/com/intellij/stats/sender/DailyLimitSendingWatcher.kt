// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.sender

import com.intellij.ide.util.PropertiesComponent
import java.text.SimpleDateFormat
import java.util.*

class DailyLimitSendingWatcher(private val dailyLimit: Int, private val info: SentDataInfo) {

  fun isLimitReached(): Boolean = info.sentToday(System.currentTimeMillis()) >= dailyLimit

  fun dataSent(size: Int) {
    info.dataSent(System.currentTimeMillis(), size)
  }

  abstract class SentDataInfo {
    companion object {
      private val FORMAT = SimpleDateFormat("yyyyMMdd")
    }

    fun sentToday(timestamp: Long): Int {
      if (!isSameDay(timestamp, latestSendingTimestamp)) {
        return 0
      }

      return sentCount
    }

    fun dataSent(timestamp: Long, bytesCount: Int) {
      sentCount = sentToday(timestamp) + bytesCount
      latestSendingTimestamp = timestamp
    }

    private fun isSameDay(ts1: Long, ts2: Long): Boolean {
      fun Long.asString(): String = FORMAT.format(Date(this))
      return ts1.asString() == ts2.asString()
    }

    protected abstract var sentCount: Int
    protected abstract var latestSendingTimestamp: Long

    class DumbInfo : SentDataInfo() {
      override var sentCount: Int = 0
      override var latestSendingTimestamp: Long = 0

    }
  }
}