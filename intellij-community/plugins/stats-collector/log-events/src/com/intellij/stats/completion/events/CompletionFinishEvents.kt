// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.completion.events

import com.intellij.stats.completion.*


class CompletionCancelledEvent(
  userId: String, sessionId: String,
  @JvmField var performance: Map<String, Long>,
  bucket: String, timestamp: Long
) : LogEvent(userId, sessionId, Action.COMPLETION_CANCELED, bucket, timestamp) {
    override fun accept(visitor: LogEventVisitor) {
        visitor.visit(this)
    }
}


class ExplicitSelectEvent(
  userId: String,
  sessionId: String,
  lookupState: LookupState,
  @JvmField var selectedId: Int,
  @JvmField var performance: Map<String, Long>,
  bucket: String,
  timestamp: Long
) : LookupStateLogData(userId, sessionId, Action.EXPLICIT_SELECT, lookupState, bucket, timestamp
) {

    override fun accept(visitor: LogEventVisitor) {
        visitor.visit(this)
    }

}


class TypedSelectEvent(
  userId: String,
  sessionId: String,
  lookupState: LookupState,
  @JvmField var selectedId: Int,
  @JvmField var performance: Map<String, Long>,
  bucket: String,
  timestamp: Long
) : LookupStateLogData(userId, sessionId, Action.TYPED_SELECT, lookupState, bucket, timestamp) {

    override fun accept(visitor: LogEventVisitor) {
        visitor.visit(this)
    }

}