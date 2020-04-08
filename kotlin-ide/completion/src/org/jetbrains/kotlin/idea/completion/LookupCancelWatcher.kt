/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.completion

import com.intellij.application.subscribe
import com.intellij.codeInsight.completion.CompletionPhaseListener
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.jetbrains.kotlin.idea.statistics.CompletionFUSCollector
import org.jetbrains.kotlin.idea.statistics.CompletionFUSCollector.completionStatsData
import org.jetbrains.kotlin.idea.statistics.FinishReasonStats

class LookupCancelWatcher : StartupActivity {

    override fun runActivity(project: Project) {
        CompletionPhaseListener.TOPIC.subscribe(project, CompletionPhaseListener { isCompletionRunning ->
            if (isCompletionRunning) {
                if (completionStatsData != null) {
                    completionStatsData = completionStatsData?.copy(finishReason = FinishReasonStats.INTERRUPTED)
                    CompletionFUSCollector.log(completionStatsData)
                    completionStatsData = null
                }
                completionStatsData = CompletionFUSCollector.CompletionStatsData(System.currentTimeMillis())
            }

            if (!isCompletionRunning) {
                completionStatsData = completionStatsData?.copy(finishTime = System.currentTimeMillis())
            }
        })

        EditorFactory.getInstance().addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorReleased(event: EditorFactoryEvent) {
                    LookupCancelService.getServiceIfCreated(project)?.disposeLastReminiscence(event.editor)
                }
            },
            project
        )

        LookupManager.getInstance(project).addPropertyChangeListener { event ->
            if (event.propertyName == LookupManager.PROP_ACTIVE_LOOKUP) {
                val lookup = event.newValue as Lookup?
                lookup?.addLookupListener(LookupCancelService.getInstance(project).lookupCancelListener)
                lookup?.addLookupListener(object : LookupListener {
                    override fun lookupShown(event: LookupEvent) {
                        completionStatsData = completionStatsData?.copy(shownTime = System.currentTimeMillis())
                    }

                    override fun lookupCanceled(event: LookupEvent) {
                        completionStatsData = completionStatsData?.copy(
                            finishReason = if (event.isCanceledExplicitly) FinishReasonStats.CANCELLED else FinishReasonStats.HIDDEN
                        )
                        CompletionFUSCollector.log(completionStatsData)
                        completionStatsData = null
                    }

                    override fun itemSelected(event: LookupEvent) {
                        val eventLookup = event.lookup
                        val lookupIndex = eventLookup.items.indexOf(eventLookup.currentItem)
                        if (lookupIndex >= 0) completionStatsData = completionStatsData?.copy(selectedItem = lookupIndex)

                        completionStatsData = completionStatsData?.copy(finishReason = FinishReasonStats.DONE)
                        CompletionFUSCollector.log(completionStatsData)
                        completionStatsData = null
                    }
                })
            }
        }
    }
}
