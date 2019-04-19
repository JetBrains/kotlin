// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.completion.events

import com.intellij.stats.completion.Action
import com.intellij.stats.completion.LogEventVisitor
import com.intellij.stats.completion.LookupEntryInfo
import com.intellij.stats.completion.LookupState


class BackspaceEvent(
        userId: String,
        sessionId: String,
        lookupState: LookupState,
        @JvmField var queryLength: Int,
        timestamp: Long)
    : LookupStateLogData(userId, sessionId, Action.BACKSPACE, lookupState, timestamp) {

    override fun accept(visitor: LogEventVisitor) {
        visitor.visit(this)
    }
}


class TypeEvent(
        userId: String,
        sessionId: String,
        lookupState: LookupState,
        @JvmField var queryLength: Int,
        timestamp: Long)
    : LookupStateLogData(userId, sessionId, Action.TYPE, lookupState, timestamp) {

    override fun accept(visitor: LogEventVisitor) {
        visitor.visit(this)
    }
}
