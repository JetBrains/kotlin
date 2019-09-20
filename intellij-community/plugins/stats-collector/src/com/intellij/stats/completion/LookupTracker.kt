// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.lang.Language
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

abstract class LookupTracker : PropertyChangeListener {
  override fun propertyChange(evt: PropertyChangeEvent?) {
    val lookup = evt?.newValue
    if (lookup is LookupImpl) {
      lookupCreated(lookup.language(), lookup)
    }
    else {
      lookupClosed()
    }
  }

  protected abstract fun lookupCreated(language: Language?, lookup: LookupImpl)

  protected abstract fun lookupClosed()
}
