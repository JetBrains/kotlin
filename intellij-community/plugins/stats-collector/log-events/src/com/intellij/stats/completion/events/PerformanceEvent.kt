// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.completion.events

import com.intellij.stats.completion.Action
import com.intellij.stats.completion.LogEventVisitor

class PerformanceEvent(userUid: String,
                       sessionUid: String,
                       @JvmField var description: String,
                       @JvmField var value: Long,
                       timestamp: Long) : LogEvent(userUid, sessionUid, Action.PERFORMANCE, timestamp) {
    override fun accept(visitor: LogEventVisitor) {
        visitor.visit(this)
    }
}