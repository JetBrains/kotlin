// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.sorting

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupCellRenderer
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.completion.settings.CompletionMLRankingSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.util.Key
import com.intellij.stats.completion.LookupTracker
import com.intellij.stats.storage.factors.LookupStorage
import com.intellij.stats.storage.factors.MutableLookupStorage
import com.intellij.ui.JBColor
import java.awt.Color
import java.util.concurrent.atomic.AtomicInteger

class PositionDiffArrowInitializer : ProjectManagerListener {
  companion object {
    val POSITION_DIFF_KEY = Key.create<AtomicInteger>("PositionChangingArrowsInitializer.POSITION_DIFF_KEY")
    const val ARROW_UP = "↑"
    const val ARROW_DOWN = "↓"
    private val ML_RANK_DIFF_GREEN_COLOR = JBColor(JBColor.GREEN.darker(), JBColor.GREEN.brighter())
  }

  override fun projectOpened(project: Project) {
    LookupManager.getInstance(project).addPropertyChangeListener(object : LookupTracker() {
      private fun shouldShowDiff(lookupStorage: LookupStorage): Boolean {
        val mlRankingSettings = CompletionMLRankingSettings.getInstance()
        return lookupStorage.model != null && mlRankingSettings.isShowDiffEnabled
      }

      override fun lookupCreated(lookup: LookupImpl, storage: MutableLookupStorage) {
        if (!shouldShowDiff(storage)) return

        lookup.addPresentationCustomizer(object : LookupCellRenderer.ItemPresentationCustomizer {
          override fun customizePresentation(item: LookupElement,
                                             presentation: LookupElementPresentation): LookupElementPresentation {
            val diff = item.getUserData(POSITION_DIFF_KEY)?.get()
            if (diff == null || diff == 0) return presentation
            val newPresentation = LookupElementPresentation()
            newPresentation.copyFrom(presentation)
            val text = if (diff < 0) " $ARROW_UP${-diff} " else " $ARROW_DOWN$diff "
            val color: Color = if (diff < 0) ML_RANK_DIFF_GREEN_COLOR else JBColor.RED
            val fragments = presentation.tailFragments
            newPresentation.setTailText(text, color)
            for (fragment in fragments) {
              newPresentation.appendTailText(fragment.text, fragment.isGrayed)
            }
            return newPresentation
          }
        })
      }
    }, project)
  }
}