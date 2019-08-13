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

import com.intellij.stats.completion.LogEventVisitor
import com.intellij.stats.completion.LookupEntryDiff
import com.intellij.stats.completion.events.*
import org.jetbrains.annotations.TestOnly

class CompletionValidationState(event: CompletionStartedEvent) : LogEventVisitor() {
    private var currentPosition = event.currentPosition
    private var completionList = event.completionListIds
    private var currentId = getSafeCurrentId(completionList, currentPosition)

    private var idToFactorNames = event.newCompletionListItems
      .associate { it.id to (it.relevance?.keys?.toMutableSet() ?: mutableSetOf()) }.toMutableMap()

    private var isValid = true
    private var isFinished = false
    private var errorMessage = ""

    private var events = mutableListOf<LogEvent>(event)

    private fun updateState(nextEvent: LookupStateLogData) {
        currentPosition = nextEvent.currentPosition

        nextEvent.newCompletionListItems.forEach {
            updateValid(it.id !in idToFactorNames, "item with ${it.id} marked as a new item twice")
            val factorNames = it.relevance?.keys ?: emptySet()
            idToFactorNames[it.id] = factorNames.toMutableSet()
        }

        if (nextEvent.completionListIds.isNotEmpty()) {
            completionList = nextEvent.completionListIds
        }

        updateFactors(nextEvent.itemsDiff)

        currentId = getSafeCurrentId(completionList, currentPosition)
    }

    private fun getSafeCurrentId(completionList: List<Int>, position: Int): Int {
        return if (completionList.isEmpty()) {
            -1
        }
        else if (position < completionList.size && position >= 0) {
            completionList[position]
        }
        else {
            invalidate("completion list size: ${completionList.size}, requested position: $position")
            -2
        }
    }

    private fun updateFactors(diffs: List<LookupEntryDiff>) {
        for (diff in diffs) {
            val id = diff.id
            val knownFactors = idToFactorNames[id]
            if (knownFactors == null) {
                invalidate("unknown element id in the diff list: $id")
                return
            }

            checkDiffValid(diff)

            diff.added.keys.forEach { name ->
                updateValid(name !in knownFactors, "factor '$name' has been added earlier")
                knownFactors.add(name)
            }

            diff.changed.keys.forEach { name -> updateValid(name in knownFactors, "changed factor '$name' not found among factors") }

            diff.removed.forEach { name ->
                updateValid(name in knownFactors, "removed factor '$name' not found among factors")
                knownFactors.remove(name)
            }
        }
    }

    private fun checkDiffValid(diff: LookupEntryDiff) {
        fun checkIntersection(first: Set<String>, second: Iterable<String>, keysDescription: String) {
            val intersection = first.intersect(second)
            if (intersection.isNotEmpty()) {
                invalidate("$keysDescription factors changed simultaneously: ")
            }
        }

        checkIntersection(diff.added.keys, diff.changed.keys, "added and changed")
        checkIntersection(diff.added.keys, diff.removed, "added and removed")
        checkIntersection(diff.changed.keys, diff.removed, "changed and removed")
    }

    fun accept(nextEvent: LogEvent) {
        events.add(nextEvent)

        if (isFinished) {
            invalidate("activity after completion finish session")
        }
        else if (isValid) {
            nextEvent.accept(this)
        }
    }

    override fun visit(event: DownPressedEvent) {
        val beforeDownPressedPosition = currentPosition
        updateState(event)
        if (completionList.isEmpty()) return
        val isCorrectPosition = (beforeDownPressedPosition + 1) % completionList.size == currentPosition
        updateValid(isCorrectPosition,
                    "position after up pressed event incorrect, before event $beforeDownPressedPosition, " +
                    "now $currentPosition, " +
                    "elements in list ${completionList.size}"
        )

    }

    private fun invalidate(error: String) = updateValid(false, error)

    private fun updateValid(value: Boolean, error: String) {
        val wasValidBefore = isValid
        isValid = isValid && value
        if (wasValidBefore && !isValid) {
            errorMessage = error
        }
    }

    override fun visit(event: UpPressedEvent) {
        val beforeUpPressedPosition = currentPosition
        updateState(event)

        if (completionList.isEmpty()) return
        val isCorrectPosition = (completionList.size + beforeUpPressedPosition - 1) % completionList.size == currentPosition
        updateValid(isCorrectPosition,
                "position after up pressed event incorrect, before event $beforeUpPressedPosition, " +
                "now $currentPosition, " +
                "elements in list ${completionList.size}"
        )
    }

    override fun visit(event: TypeEvent) {
        updateState(event)
        updateValid(idToFactorNames.keys.containsAll(completionList),
                "TypeEvent: some elements in completion list are not registered")
    }

    override fun visit(event: BackspaceEvent) {
        updateState(event)
        updateValid(idToFactorNames.keys.containsAll(completionList),
                "Backspace: some elements in completion list are not registered")
    }

    override fun visit(event: ExplicitSelectEvent) {
        val selectedIdBefore = currentId
        updateState(event)

        updateValid(selectedIdBefore == currentId && currentId in idToFactorNames,
                "Selected element was not registered")

        isFinished = true
    }

    override fun visit(event: CompletionCancelledEvent) {
        isFinished = true
    }

    override fun visit(event: TypedSelectEvent) {
        val id = event.selectedId
        updateValid(completionList[currentPosition] == id,
                "Element selected by typing is not the same id")

        isFinished = true
    }


    fun isSessionValid(): Boolean {
        return isValid && isFinished
    }

    @TestOnly
    fun isCurrentlyValid(): Boolean {
        return isValid
    }

    fun errorMessage(): String {
        return if (isValid && !isFinished) {
            "Session was not finished"
        }
        else {
            errorMessage
        }
    }

}