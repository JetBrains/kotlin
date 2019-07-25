/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.stats.completion.events

import com.intellij.stats.completion.*


abstract class LogEvent(
        @Transient var userUid: String,
        @Transient var sessionUid: String,
        @Transient var actionType: Action,
        @Transient var timestamp: Long
) {

    @Transient var recorderId: String = "completion-stats"
    @Transient var recorderVersion: String = "6"
    @Transient var bucket: String = "-1"
    var validationStatus: ValidationStatus = ValidationStatus.UNKNOWN

    abstract fun accept(visitor: LogEventVisitor)
}


abstract class LookupStateLogData(
        userId: String,
        sessionId: String,
        action: Action,
        state: LookupState,
        timestamp: Long
) : LogEvent(userId, sessionId, action, timestamp) {

    @JvmField var completionListIds: List<Int> = state.ids
    @JvmField var newCompletionListItems: List<LookupEntryInfo> = state.newItems
    @JvmField var itemsDiff: List<LookupEntryDiff> = state.itemsDiff
    @JvmField var currentPosition: Int = state.selectedPosition

    @JvmField var originalCompletionType: String = ""
    @JvmField var originalInvokationCount: Int = -1

}