// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.stats.PerformanceTracker

class LoggerPerformanceTracker(
  private val delegate: CompletionActionsListener,
  private val tracker: PerformanceTracker)
  : CompletionActionsListener {
  override fun beforeDownPressed() = measureAndLog("beforeDownPressed") {
    delegate.beforeDownPressed()
  }

  override fun downPressed() = measureAndLog("downPressed") {
    delegate.downPressed()
  }

  override fun beforeUpPressed() = measureAndLog("beforeUpPressed") {
    delegate.beforeUpPressed()
  }

  override fun upPressed() = measureAndLog("upPressed") {
    delegate.upPressed()
  }

  override fun beforeBackspacePressed() = measureAndLog("beforeBackspacePressed") {
    delegate.beforeBackspacePressed()
  }

  override fun afterBackspacePressed() = measureAndLog("afterBackspacePressed") {
    delegate.afterBackspacePressed()
  }

  override fun beforeCharTyped(c: Char) = measureAndLog("beforeCharTyped") {
    delegate.beforeCharTyped(c)
  }

  override fun lookupShown(event: LookupEvent) = measureAndLog("lookupShown") {
    delegate.lookupShown(event)
  }

  override fun afterAppend(c: Char) = measureAndLog("afterAppend") {
    delegate.afterAppend(c)
  }

  override fun afterTruncate() = measureAndLog("afterTruncate") {
    delegate.afterTruncate()
  }

  override fun beforeTruncate() = measureAndLog("beforeTruncate") {
    delegate.beforeTruncate()
  }

  override fun beforeAppend(c: Char) = measureAndLog("beforeAppend") {
    delegate.beforeAppend(c)
  }

  override fun beforeItemSelected(event: LookupEvent): Boolean = measureAndLog("beforeItemSelected") {
    delegate.beforeItemSelected(event)
  }

  override fun itemSelected(event: LookupEvent) = measureAndLog("itemSelected") {
    delegate.itemSelected(event)
  }

  override fun lookupCanceled(event: LookupEvent) = measureAndLog("lookupCanceled") {
    delegate.lookupCanceled(event)
  }

  override fun currentItemChanged(event: LookupEvent) = measureAndLog("currentItemChanged") {
    delegate.currentItemChanged(event)
  }

  private inline fun <T> measureAndLog(actionName: String, block: () -> T): T {
    val start = System.currentTimeMillis()
    val c = block()
    tracker.eventLogged(actionName, System.currentTimeMillis() - start)
    return c
  }
}