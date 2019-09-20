// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.sender

import com.intellij.ide.util.PropertiesComponent

class PersistentSentDataInfo(private val properties: PropertiesComponent) : DailyLimitSendingWatcher.SentDataInfo() {
  companion object {
    private const val SENT_DATA_SIZE_TODAY_PROPERTY = "stats.collector.today.sent.data.size"
    private const val DATA_SENT_TIMESTAMP_PROPERTY = "stats.collector.latest.sending.timestamp"
  }

  override var sentCount: Int
    get() = properties.getInt(SENT_DATA_SIZE_TODAY_PROPERTY, 0)
    set(value) = properties.setValue(SENT_DATA_SIZE_TODAY_PROPERTY, value, 0)

  override var latestSendingTimestamp: Long
    get() = properties.getOrInitLong(DATA_SENT_TIMESTAMP_PROPERTY, 0L)
    set(value) = properties.setValue(DATA_SENT_TIMESTAMP_PROPERTY, value.toString())
}