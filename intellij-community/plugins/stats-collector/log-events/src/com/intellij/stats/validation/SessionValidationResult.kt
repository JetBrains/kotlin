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


interface SessionValidationResult {
    fun addErrorSession(errorSession: List<EventLine>)
    fun addValidSession(validSession: List<EventLine>)
}


open class SimpleSessionValidationResult: SessionValidationResult {
    private val error = mutableListOf<String>()
    private val valid = mutableListOf<String>()

    val errorLines: List<String>
        get() = error

    val validLines: List<String>
        get() = valid

    override fun addErrorSession(errorSession: List<EventLine>) {
        error.addAll(errorSession.map { it.originalLine })
    }

    override fun addValidSession(validSession: List<EventLine>) {
        valid.addAll(validSession.map { it.originalLine })
    }
}
