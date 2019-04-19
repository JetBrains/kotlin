// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion

import com.google.common.annotations.VisibleForTesting
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.featureStatistics.FeatureUsageTracker
import com.intellij.featureStatistics.FeatureUsageTrackerImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.statistics.StatisticsInfo
import com.intellij.util.Alarm

/**
 * @author peter
 */
class StatisticsUpdate
    private constructor(private val myInfo: StatisticsInfo) : Disposable {
  private var mySpared: Int = 0

  override fun dispose() {}

  fun addSparedChars(lookup: Lookup, item: LookupElement, context: InsertionContext) {
    val textInserted: String
    if (context.offsetMap.containsOffset(CompletionInitializationContext.START_OFFSET) &&
        context.offsetMap.containsOffset(InsertionContext.TAIL_OFFSET) &&
        context.tailOffset >= context.startOffset) {
      textInserted = context.document.immutableCharSequence.subSequence(context.startOffset, context.tailOffset).toString()
    }
    else {
      textInserted = item.lookupString
    }
    val withoutSpaces = StringUtil.replace(textInserted, listOf(" ", "\t", "\n"), listOf("", "", ""))
    var spared = withoutSpaces.length - lookup.itemPattern(item).length
    val completionChar = context.completionChar
    if (!LookupEvent.isSpecialCompletionChar(completionChar) && withoutSpaces.contains(completionChar.toString())) {
      spared--
    }
    if (spared > 0) {
      mySpared += spared
    }
  }

  fun trackStatistics(context: InsertionContext) {
    if (ourPendingUpdate !== this) {
      return
    }

    if (!context.offsetMap.containsOffset(CompletionInitializationContext.START_OFFSET)) {
      return
    }

    val document = context.document
    val startOffset = context.startOffset
    val tailOffset =
      if (context.editor.selectionModel.hasSelection()) context.editor.selectionModel.selectionStart
      else context.editor.caretModel.offset
    if (startOffset < 0 || tailOffset <= startOffset) {
      return
    }

    val marker = document.createRangeMarker(startOffset, tailOffset)
    val listener = object : DocumentListener {
      override fun beforeDocumentChange(e: DocumentEvent) {
        if (!marker.isValid || e.offset > marker.startOffset && e.offset < marker.endOffset) {
          cancelLastCompletionStatisticsUpdate()
        }
      }
    }

    ourStatsAlarm.addRequest({
                               if (ourPendingUpdate === this) {
                                 applyLastCompletionStatisticsUpdate()
                               }
                             }, 20 * 1000)

    document.addDocumentListener(listener)
    Disposer.register(this, Disposable {
      document.removeDocumentListener(listener)
      marker.dispose()
      ourStatsAlarm.cancelAllRequests()
    })
  }

  companion object {
    private val ourStatsAlarm = Alarm(ApplicationManager.getApplication())
    private var ourPendingUpdate: StatisticsUpdate? = null

    init {
      Disposer.register(ApplicationManager.getApplication(), Disposable { cancelLastCompletionStatisticsUpdate() })
    }

    @VisibleForTesting
    @JvmStatic
    fun collectStatisticChanges(item: LookupElement): StatisticsUpdate {
      applyLastCompletionStatisticsUpdate()

      val base = StatisticsWeigher.getBaseStatisticsInfo(item, null)
      if (base === StatisticsInfo.EMPTY) {
        return StatisticsUpdate(StatisticsInfo.EMPTY)
      }

      val update = StatisticsUpdate(base)
      ourPendingUpdate = update
      Disposer.register(update, Disposable { ourPendingUpdate = null })

      return update
    }

    @JvmStatic
    fun cancelLastCompletionStatisticsUpdate() {
      ourPendingUpdate?.let { Disposer.dispose(it) }
      assert(ourPendingUpdate == null)
    }

    @JvmStatic
    fun applyLastCompletionStatisticsUpdate() {
      ourPendingUpdate?.let {
        it.myInfo.incUseCount()
        (FeatureUsageTracker.getInstance() as FeatureUsageTrackerImpl).completionStatistics.registerInvocation(it.mySpared)
      }
      cancelLastCompletionStatisticsUpdate()
    }
  }
}
