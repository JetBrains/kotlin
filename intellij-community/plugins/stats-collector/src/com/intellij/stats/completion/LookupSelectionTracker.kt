// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.stats.personalization.session.*

class LookupSelectionTracker(private val storage: MutableLookupFactorsStorage) : LookupListener {
  private var currentElementStorage: MutableElementFactorsStorage? = null
  override fun lookupShown(event: LookupEvent) = selectionChanged(event)
  override fun currentItemChanged(event: LookupEvent) = selectionChanged(event)

  private fun selectionChanged(event: LookupEvent) {
    val lookupElement = event.item
    if (lookupElement != null) {
      val elementStorage = storage.getItemStorage(lookupElement.idString())

      if (elementStorage == currentElementStorage) return

      elementStorage.selectionTracker.itemSelected()
      currentElementStorage?.selectionTracker?.itemUnselected()
      currentElementStorage = elementStorage
    }
  }
}