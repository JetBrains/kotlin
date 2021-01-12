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

package com.intellij.stats.validation

import com.intellij.stats.completion.LogEventSerializer
import com.intellij.stats.completion.events.CompletionStartedEvent

class InputSessionValidator(private val sessionValidationResult: SessionValidationResult) {

    fun validate(input: Iterable<String>) {
        var currentSessionUid: String? = null
        val session = mutableListOf<EventLine>()

        for (line in input) {
            if (line.trim().isEmpty()) continue

            val event = LogEventSerializer.fromString(line)
            val eventLine = EventLine(event, line)

            if (eventLine.sessionUid == currentSessionUid) {
                session.add(eventLine)
            }
            else {
                processCompletionSession(session)
                session.clear()
                currentSessionUid = eventLine.sessionUid
                session.add(eventLine)
            }
        }

        processCompletionSession(session)
    }


    private fun processCompletionSession(session: List<EventLine>) {
        if (session.isEmpty()) return
        if (session.any { !it.isValid }) {
            dumpSession(session, isValidSession = false, errorMessage = "Event line is invalid")
            return
        }

        var isValidSession = false
        var errorMessage = ""
        val initial = session.first()
        if (initial.event is CompletionStartedEvent) {
            val state = CompletionValidationState(initial.event)
            session.drop(1).forEach { state.accept(it.event!!) }
            isValidSession = state.isSessionValid()
            if (!isValidSession) {
                errorMessage = state.errorMessage()
            }
        }
        else {
            errorMessage = "Session starts with other event: ${initial.event?.actionType}"
        }

        dumpSession(session, isValidSession, errorMessage)
    }

    private fun dumpSession(session: List<EventLine>, isValidSession: Boolean,
                            @Suppress("UNUSED_PARAMETER") errorMessage: String) {
        if (isValidSession) {
            sessionValidationResult.addValidSession(session)
        }
        else {
            sessionValidationResult.addErrorSession(session)
        }
    }

}
