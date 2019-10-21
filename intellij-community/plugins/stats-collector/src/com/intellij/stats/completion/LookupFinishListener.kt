// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.codeInsight.lookup.impl.LookupImpl

abstract class LookupFinishListener : LookupListener {
  override fun itemSelected(event: LookupEvent) {
    val lookup = event.lookup as? LookupImpl ?: return
    val element = event.item ?: return
    if (isSelectedByTyping(lookup, element)) {
      typedSelect(lookup, element)
    }
    else {
      explicitSelect(lookup, element)
    }
  }

  override fun lookupCanceled(event: LookupEvent) {
    val lookup = event.lookup as? LookupImpl ?: return
    val element = lookup.currentItem
    if (element != null && isSelectedByTyping(lookup, element)) {
      typedSelect(lookup, element)
    }
    else {
      cancelled(lookup, event.isCanceledExplicitly)
    }
  }

  abstract fun cancelled(lookup: LookupImpl, canceledExplicitly: Boolean)
  abstract fun explicitSelect(lookup: LookupImpl, element: LookupElement)
  abstract fun typedSelect(lookup: LookupImpl, element: LookupElement)

  private fun isSelectedByTyping(lookup: LookupImpl, element: LookupElement): Boolean = element.lookupString == lookup.itemPattern(element)
}
