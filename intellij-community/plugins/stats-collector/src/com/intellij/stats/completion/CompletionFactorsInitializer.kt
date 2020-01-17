// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.reporting.isUnitTestMode
import com.intellij.stats.personalization.UserFactorDescriptions
import com.intellij.stats.personalization.UserFactorStorage
import com.intellij.stats.personalization.UserFactorsManager
import com.intellij.stats.personalization.session.SessionFactorsUtils
import com.intellij.stats.personalization.session.SessionPrefixTracker
import com.intellij.stats.storage.factors.MutableLookupStorage

class CompletionFactorsInitializer : LookupTracker() {
  override fun lookupCreated(lookup: LookupImpl, storage: MutableLookupStorage) {
    if (isUnitTestMode() && !CompletionTrackerInitializer.isEnabledInTests) return

    processUserFactors(lookup, storage)
    processSessionFactors(lookup, storage)
  }

  override fun lookupClosed() {
  }

  private fun shouldUseUserFactors() = UserFactorsManager.ENABLE_USER_FACTORS

  private fun shouldUseSessionFactors(): Boolean = SessionFactorsUtils.shouldUseSessionFactors()

  private fun processUserFactors(lookup: LookupImpl,
                                 lookupStorage: MutableLookupStorage) {
    if (!shouldUseUserFactors()) return

    lookupStorage.initUserFactors(lookup.project)

    UserFactorStorage.applyOnBoth(lookup.project, UserFactorDescriptions.COMPLETION_USAGE) {
      it.fireCompletionUsed()
    }

    // setPrefixChangeListener has addPrefixChangeListener semantics
    lookup.setPrefixChangeListener(TimeBetweenTypingTracker(lookup.project))
    lookup.addLookupListener(LookupCompletedTracker())
    lookup.addLookupListener(LookupStartedTracker())
  }

  private fun processSessionFactors(lookup: LookupImpl, lookupStorage: MutableLookupStorage) {
    if (!shouldUseSessionFactors()) return

    lookup.setPrefixChangeListener(SessionPrefixTracker(lookupStorage.sessionFactors))
    lookup.addLookupListener(LookupSelectionTracker(lookupStorage))
  }
}