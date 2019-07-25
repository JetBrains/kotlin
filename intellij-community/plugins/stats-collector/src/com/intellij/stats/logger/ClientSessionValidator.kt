/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.intellij.stats.logger

import com.intellij.stats.completion.LogEventSerializer
import com.intellij.stats.completion.ValidationStatus
import com.intellij.stats.completion.events.LogEvent
import com.intellij.stats.validation.InputSessionValidator
import com.intellij.stats.validation.SimpleSessionValidationResult

/**
 * @author Vitaliy.Bibaev
 */
class ClientSessionValidator : SessionValidator {
    override fun validate(session: List<LogEvent>) {
        val validationResult = SimpleSessionValidationResult()
        val line2event = session.associateTo(linkedMapOf(), { LogEventSerializer.toString(it) to it })
        InputSessionValidator(validationResult).validate(line2event.keys.toList())
        validationResult.errorLines.forEach { line2event[it]!!.validationStatus = ValidationStatus.INVALID }
        validationResult.validLines.forEach { line2event[it]!!.validationStatus = ValidationStatus.VALID }
    }
}