// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.impl.PrefixChangeListener
import com.intellij.ide.ui.UISettings
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.stats.storage.factors.LookupStorage
import com.intellij.stats.storage.factors.MutableLookupStorage

class CompletionQualityTracker : LookupTracker() {
  companion object {
    private const val GROUP_ID = "completion"
  }

  override fun lookupCreated(lookup: LookupImpl, storage: MutableLookupStorage) {
    val queryTracker = QueryTracker()
    lookup.addPrefixChangeListener(queryTracker, lookup)
    lookup.addLookupListener(CompletionQualityListener(storage, queryTracker))
  }

  override fun lookupClosed() {
  }

  private class QueryTracker : PrefixChangeListener {
    var typingActionsCount: Int = 0
      private set
    var backspaceActionsCount: Int = 0
      private set

    override fun beforeTruncate() {
      backspaceActionsCount += 1
    }

    override fun beforeAppend(c: Char) {
      typingActionsCount += 1
    }
  }

  private class CompletionQualityListener(private val storage: LookupStorage,
                                          private val queryTracker: QueryTracker) : LookupFinishListener() {
    var shownTimestamp: Long = -1L
    var selectionChangesCount: Int = 0
    override fun lookupShown(event: LookupEvent) {
      shownTimestamp = System.currentTimeMillis()
    }

    override fun currentItemChanged(event: LookupEvent) {
      selectionChangesCount += 1
    }

    override fun cancelled(lookup: LookupImpl, canceledExplicitly: Boolean) {
      logDetails(lookup, if (canceledExplicitly) FinishType.CANCELED_EXPLICITLY else FinishType.CANCELED_BY_TYPING)
    }

    override fun explicitSelect(lookup: LookupImpl, element: LookupElement) {
      logDetails(lookup, FinishType.EXPLICIT, element)
    }

    override fun typedSelect(lookup: LookupImpl, element: LookupElement) {
      logDetails(lookup, FinishType.TYPED, element)
    }

    private fun logDetails(lookup: LookupImpl,
                           finishType: FinishType,
                           currentItem: LookupElement? = null) {
      val data = FeatureUsageData().apply {
        // Diagnostics
        addLanguage(storage.language)
        addData("alphabetically", UISettings.instance.sortLookupElementsLexicographically)

        // details
        addData("ml_used", storage.mlUsed())
        addData("version", storage.model?.version() ?: "unknown")
        addData("token_length", currentItem?.lookupString?.length ?: -1)
        addData("query_length", if (currentItem == null) -1 else lookup.itemPattern(currentItem).length)

        // Quality
        addData("selected_index", lookup.selectedIndex)
        addData("finish_type", finishType.toString())
        addData("duration", System.currentTimeMillis() - storage.startedTimestamp)
        addData("selection_changed", selectionChangesCount)
        addData("typing", queryTracker.typingActionsCount)
        addData("backspaces", queryTracker.backspaceActionsCount)

        // Performance
        addData("total_ml_time", storage.performanceTracker.totalMLTimeContribution())
        addData("time_to_show", if (shownTimestamp == -1L) -1 else shownTimestamp - storage.startedTimestamp)
      }

      FUCounterUsageLogger.getInstance().logEvent(GROUP_ID, "finished", data)
    }

    private enum class FinishType {
      TYPED, EXPLICIT, CANCELED_EXPLICITLY, CANCELED_BY_TYPING
    }
  }
}
