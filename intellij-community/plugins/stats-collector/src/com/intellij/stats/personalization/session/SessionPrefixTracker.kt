// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.personalization.session

import com.intellij.codeInsight.lookup.impl.PrefixChangeListener

class SessionPrefixTracker(private val storage: LookupSessionFactorsStorage) : PrefixChangeListener {
  override fun afterAppend(c: Char) {
    storage.queryTracker.afterAppend(c)
  }

  override fun afterTruncate() {
    storage.queryTracker.afterTruncate()
  }
}