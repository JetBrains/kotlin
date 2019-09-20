// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.completion.events

import com.intellij.stats.completion.Action
import com.intellij.stats.completion.LogEventVisitor


class CustomMessageEvent(userId: String, sessionId: String, @JvmField var text: String, bucket: String, timestamp: Long)
    : LogEvent(userId, sessionId, Action.CUSTOM, bucket, timestamp) {

    override fun accept(visitor: LogEventVisitor) {
        visitor.visit(this)
    }

}