// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

interface LookupElementFactorsStorage {
  fun lastUsedLookupFactors(): Map<String, Any>
  fun lastUsedElementFactors(): Map<String, Any>

  fun getVisiblePosition(): Int

  val selectionTracker: CompletionSelectionTracker
}