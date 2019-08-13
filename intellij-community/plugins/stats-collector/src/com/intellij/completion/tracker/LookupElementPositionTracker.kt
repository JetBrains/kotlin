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
package com.intellij.completion.tracker

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Key
import com.intellij.stats.completion.idString
import com.intellij.stats.completion.ElementPositionHistory
import com.intellij.stats.completion.StagePosition


interface LookupElementPositionTracker {
    fun positionsHistory(lookup: LookupImpl, element: LookupElement): List<StagePosition>

    companion object {
        fun getInstance(): LookupElementPositionTracker = service()
    }
}


class UserDataLookupElementPositionTracker : LookupElementPositionTracker {

    override fun positionsHistory(lookup: LookupImpl, element: LookupElement): List<StagePosition> {
        val id = element.idString()
        val userData = lookup.getUserData(KEY)
        return userData?.get(id)?.history() ?: emptyList()
    }

    companion object {
        private val KEY = Key.create<MutableMap<String, ElementPositionHistory>>("lookup.element.position.history")

        fun history(lookup: LookupImpl): MutableMap<String, ElementPositionHistory>? = lookup.getUserData(KEY)

        fun addElementPosition(lookup: LookupImpl, element: LookupElement, stagePosition: StagePosition) {
            lookup.putUserDataIfAbsent(KEY, mutableMapOf())
            val elementsHistory = lookup.getUserData(KEY)!!

            val id = element.idString()
            val history = elementsHistory.computeIfAbsent(id, { ElementPositionHistory() })

            history.add(stagePosition)
        }
    }

}