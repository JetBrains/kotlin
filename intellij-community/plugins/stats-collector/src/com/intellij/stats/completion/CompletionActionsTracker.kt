// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.impl.PrefixChangeListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.stats.experiment.WebServiceStatus

class CompletionActionsTracker(private val lookup: LookupImpl,
                               private val logger: CompletionLogger,
                               private val experimentHelper: WebServiceStatus)
    : CompletionPopupListener,
      PrefixChangeListener,
      LookupListener {
    private companion object {
        const val INITIAL_CONTRIBUTION_KEY = "ml.contribution.initial"
        const val CURRENT_CONTRIBUTION_KEY = "ml.contribution.current"
        const val TOTAL_CONTRIBUTION_KEY = "ml.contribution.total"
    }

    private var completionStarted = false
    private var selectedByDotTyping = false

    private val deferredLog = DeferredLog()

    private fun isCompletionActive(): Boolean {
        return completionStarted && !lookup.isLookupDisposed
                || ApplicationManager.getApplication().isUnitTestMode
    }

    override fun lookupCanceled(event: LookupEvent) {
        if (!completionStarted) return

        val timestamp = System.currentTimeMillis()
        val mlTimeContribution = CompletionUtil.getMLTimeContribution(lookup)
        val items = lookup.items
        val currentItem = lookup.currentItem
        if (currentItem == null) {
            deferredLog.clear()
            logPerformance(TOTAL_CONTRIBUTION_KEY, mlTimeContribution, timestamp - 1)
            logger.completionCancelled(timestamp)
            return
        }

        val prefix = lookup.itemPattern(currentItem)
        val wasTyped = items.firstOrNull()?.lookupString?.equals(prefix) ?: false
        if (wasTyped || selectedByDotTyping) {
            deferredLog.log()
            logPerformance(TOTAL_CONTRIBUTION_KEY, mlTimeContribution, timestamp - 1)
            logger.itemSelectedByTyping(lookup, timestamp)
        } else {
            deferredLog.clear()
            logPerformance(TOTAL_CONTRIBUTION_KEY, mlTimeContribution, timestamp - 1)
            logger.completionCancelled(timestamp)
        }
    }

    override fun currentItemChanged(event: LookupEvent) {
        if (completionStarted) {
            return
        }

        val timestamp = System.currentTimeMillis()
        val mlTimeContribution = CompletionUtil.getMLTimeContribution(lookup)
        completionStarted = true
        deferredLog.defer {
            val isPerformExperiment = experimentHelper.isExperimentOnCurrentIDE()
            val experimentVersion = experimentHelper.experimentVersion()
            logger.completionStarted(lookup, isPerformExperiment, experimentVersion,
                                     timestamp, mlTimeContribution ?: -1)
            logPerformance(INITIAL_CONTRIBUTION_KEY, mlTimeContribution, timestamp + 1)
        }
    }

    override fun itemSelected(event: LookupEvent) {
        if (!completionStarted) return

        val timestamp = System.currentTimeMillis()
        deferredLog.log()
        logPerformance(TOTAL_CONTRIBUTION_KEY, CompletionUtil.getMLTimeContribution(lookup), timestamp - 1)
        logger.itemSelectedCompletionFinished(lookup, timestamp)
    }

    override fun beforeDownPressed() {
        deferredLog.log()
    }

    override fun downPressed() {
        if (!isCompletionActive()) return

        val timestamp = System.currentTimeMillis()
        deferredLog.log()
        deferredLog.defer {
            logger.downPressed(lookup, timestamp)
        }
    }

    override fun beforeUpPressed() {
        deferredLog.log()
    }

    override fun upPressed() {
        if (!isCompletionActive()) return

        val timestamp = System.currentTimeMillis()
        deferredLog.log()
        deferredLog.defer {
            logger.upPressed(lookup, timestamp)
        }
    }

    override fun beforeBackspacePressed() {
        if (!isCompletionActive()) return
        deferredLog.log()
    }

    override fun afterBackspacePressed() {
        if (!isCompletionActive()) return

        val timestamp = System.currentTimeMillis()
        deferredLog.log()
        deferredLog.defer {
            logger.afterBackspacePressed(lookup, timestamp)
        }
    }

    override fun beforeCharTyped(c: Char) {
        if (!isCompletionActive()) return

        val timestamp = System.currentTimeMillis()
        deferredLog.log()

        if (c == '.') {
            val item = lookup.currentItem
            if (item == null) {
                logger.customMessage("Before typed $c lookup.currentItem is null; lookup size: ${lookup.items.size}", timestamp)
                return
            }
            val text = lookup.itemPattern(item)
            if (item.lookupString == text) {
                selectedByDotTyping = true
            }
        }
    }


    override fun afterAppend(c: Char) {
        if (!isCompletionActive()) return

        val timestamp = System.currentTimeMillis()
        val mlTimeContribution = CompletionUtil.getMLTimeContribution(lookup)
        deferredLog.log()
        deferredLog.defer {
            logger.afterCharTyped(c, lookup, timestamp)
            logPerformance(CURRENT_CONTRIBUTION_KEY, mlTimeContribution, timestamp)
        }
    }

    private fun logPerformance(key: String, measure: Long?, timestamp: Long) {
        if (measure != null) {
            logger.performanceMessage(key, measure, timestamp)
        }
    }
}
