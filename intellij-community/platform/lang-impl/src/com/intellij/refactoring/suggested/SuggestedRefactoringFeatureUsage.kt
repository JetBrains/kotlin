// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.suggested

import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger

@Suppress("HardCodedStringLiteral")
internal object SuggestedRefactoringFeatureUsage {
  private const val featureUsageGroup = "suggested.refactorings"

  private var lastFeatureUsageIdLogged: Int? = null

  private const val REFACTORING_SUGGESTED = "suggested"
  const val POPUP_SHOWN = "popup.shown"
  const val POPUP_CANCELED = "popup.canceled"
  const val REFACTORING_PERFORMED = "performed"

  fun logEvent(
    eventIdSuffix: String,
    refactoringData: SuggestedRefactoringData,
    state: SuggestedRefactoringState,
    actionPlace: String?
  ) {
    val featureUsageData = FeatureUsageData().apply {
      addPlace(actionPlace)
      addData("id", state.featureUsageId)
      addLanguage(refactoringData.declaration.language)
      addData("declaration_type", refactoringData.declaration.javaClass.name)
    }
    val eventIdPrefix = when (refactoringData) {
      is SuggestedRenameData -> "rename."
      is SuggestedChangeSignatureData -> "changeSignature."
    }
    val eventId = eventIdPrefix + eventIdSuffix
    FUCounterUsageLogger.getInstance().logEvent(refactoringData.declaration.project, featureUsageGroup, eventId, featureUsageData)
  }

  fun refactoringSuggested(refactoringData: SuggestedRefactoringData, state: SuggestedRefactoringState) {
    if (state.featureUsageId != lastFeatureUsageIdLogged) {
      lastFeatureUsageIdLogged = state.featureUsageId
      logEvent(REFACTORING_SUGGESTED, refactoringData, state, null)
    }
  }
}