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
package com.intellij.stats.completion


import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.completion.sorting.RankingSupport
import com.intellij.completion.tracker.LookupElementPositionTracker
import com.intellij.ide.plugins.PluginManager
import com.intellij.stats.completion.events.*
import com.intellij.stats.personalization.UserFactorsManager


class CompletionFileLogger(private val installationUID: String,
                           private val completionUID: String,
                           private val eventLogger: CompletionEventLogger) : CompletionLogger() {

    private val stateManager = LookupStateManager()

    override fun completionStarted(lookup: LookupImpl, isExperimentPerformed: Boolean, experimentVersion: Int,
                                   timestamp: Long, mlTimeContribution: Long) {
        val state = stateManager.update(lookup, false)

        val language = lookup.language()

        val ideVersion = PluginManager.BUILD_NUMBER ?: "ideVersion"
        val pluginVersion = calcPluginVersion() ?: "pluginVersion"
        val mlRankingVersion = RankingSupport.getRanker(language)?.version() ?: "NONE"

        val userFactors = lookup.getUserData(UserFactorsManager.USER_FACTORS_KEY) ?: emptyMap()

        val event = CompletionStartedEvent(
                ideVersion, pluginVersion, mlRankingVersion,
                installationUID, completionUID,
                language?.displayName,
                isExperimentPerformed, experimentVersion,
                state, userFactors,
                queryLength = lookup.prefixLength(),
                timestamp = lookup.getUserData(CompletionUtil.COMPLETION_STARTING_TIME_KEY) ?: timestamp)

        val shownTimestamp = CompletionUtil.getShownTimestamp(lookup)
        if (shownTimestamp != null) {
            event.lookupShownTime = shownTimestamp
        }

        event.mlTimeContribution = mlTimeContribution
        event.isOneLineMode = lookup.editor.isOneLineMode
        event.isAutoPopup = CompletionUtil.getCurrentCompletionParameters()?.isAutoPopup
        event.fillCompletionParameters()

        eventLogger.log(event)
    }

    override fun customMessage(message: String, timestamp: Long) {
        val event = CustomMessageEvent(installationUID, completionUID, message, timestamp)
        eventLogger.log(event)
    }

    override fun performanceMessage(description: String, value: Long, timestamp: Long) {
        eventLogger.log(PerformanceEvent(installationUID, completionUID, description, value, timestamp))
    }

    override fun afterCharTyped(c: Char, lookup: LookupImpl, timestamp: Long) {
        val state = stateManager.update(lookup, true)
        val event = TypeEvent(installationUID, completionUID, state, lookup.prefixLength(), timestamp)
        event.fillCompletionParameters()

        eventLogger.log(event)
    }

    override fun downPressed(lookup: LookupImpl, timestamp: Long) {
        val state = stateManager.update(lookup, false)
        val event = DownPressedEvent(installationUID, completionUID, state, timestamp)
        event.fillCompletionParameters()

        eventLogger.log(event)
    }

    override fun upPressed(lookup: LookupImpl, timestamp: Long) {
        val state = stateManager.update(lookup, false)
        val event = UpPressedEvent(installationUID, completionUID, state, timestamp)
        event.fillCompletionParameters()

        eventLogger.log(event)
    }

    override fun completionCancelled(timestamp: Long) {
        val event = CompletionCancelledEvent(installationUID, completionUID, timestamp)
        eventLogger.log(event)
    }

    override fun itemSelectedByTyping(lookup: LookupImpl, timestamp: Long) {
        val state = stateManager.update(lookup, true)

        val history = lookup.itemsHistory()

        val event = TypedSelectEvent(installationUID, completionUID, state, state.selectedId, history, timestamp)
        event.fillCompletionParameters()

        eventLogger.log(event)
    }

    override fun itemSelectedCompletionFinished(lookup: LookupImpl, timestamp: Long) {
        val state = stateManager.update(lookup, true)
        val history = lookup.itemsHistory()

        val event = ExplicitSelectEvent(installationUID, completionUID, state, state.selectedId, history, timestamp)
        event.fillCompletionParameters()

        eventLogger.log(event)
    }

    private fun LookupImpl.itemsHistory(): Map<Int, ElementPositionHistory> {
        val positionTracker = LookupElementPositionTracker.getInstance()
        return items.map { stateManager.getElementId(it)!! to ElementPositionHistory(positionTracker.positionsHistory(this, it)) }.toMap()
    }

    override fun afterBackspacePressed(lookup: LookupImpl, timestamp: Long) {
        val state = stateManager.update(lookup, true)

        val event = BackspaceEvent(installationUID, completionUID, state, lookup.prefixLength(), timestamp)
        event.fillCompletionParameters()

        eventLogger.log(event)
    }

}


private fun calcPluginVersion(): String? {
    val className = CompletionStartedEvent::class.java.name
    val id = PluginManager.getPluginByClassName(className)
    val plugin = PluginManager.getPlugin(id)
    return plugin?.version
}


private val LookupState.selectedId: Int
    get() = if (selectedPosition == -1) -1 else ids[selectedPosition]

private fun LookupStateLogData.fillCompletionParameters() {
    val params = CompletionUtil.getCurrentCompletionParameters()

    originalCompletionType = params?.completionType?.toString() ?: ""
    originalInvokationCount = params?.invocationCount ?: -1
}