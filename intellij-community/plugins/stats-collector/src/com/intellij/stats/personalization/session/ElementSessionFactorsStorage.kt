// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

class ElementSessionFactorsStorage {
  private var visiblePosition: Int = -1

  private var elementFactors: Map<String, Any> = emptyMap()

  fun lastUsedElementFactors(): Map<String, Any> = elementFactors

  fun getVisiblePosition(): Int = visiblePosition

  val selectionTracker: CompletionSelectionTrackerImpl = CompletionSelectionTrackerImpl()

  fun updateUsedSessionFactors(visiblePosition: Int, elementFactors: Map<String, Any>) {
    this.visiblePosition = visiblePosition
    this.elementFactors = elementFactors
  }
}