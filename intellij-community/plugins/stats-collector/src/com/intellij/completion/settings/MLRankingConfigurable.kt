// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.settings

import com.intellij.completion.StatsCollectorBundle
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.*
import com.intellij.util.PlatformUtils

class MLRankingConfigurable(private val supportedLanguages: List<String>)
  : BoundConfigurable("ML Ranking") {
  private val settings = CompletionMLRankingSettings.getInstance()

  override fun createPanel(): DialogPanel {
    return panel {
      var enableRankingCheckbox: CellBuilder<JBCheckBox>? = null
      titledRow(StatsCollectorBundle.message("ml.completion.settings.group")) {
        row {
          val enableRanking = checkBox(StatsCollectorBundle.message("ml.completion.enable"), settings::isRankingEnabled,
                                       { settings.isRankingEnabled = it })
          for (language in supportedLanguages) {
            row {
              checkBox(language, { settings.isLanguageEnabled(language) }, { settings.setLanguageEnabled(language, it) })
                .enableIf(enableRanking.selected)
            }.apply { if (language === supportedLanguages.last()) largeGapAfter() }
          }
          enableRankingCheckbox = enableRanking
          row {
            enableRankingCheckbox?.let { enableRanking ->
              checkBox(StatsCollectorBundle.message("ml.completion.show.diff"),
                       { settings.isShowDiffEnabled },
                       { settings.isShowDiffEnabled = it }).enableIf(enableRanking.selected)
            }
          }
        }
      }
    }
  }
}
