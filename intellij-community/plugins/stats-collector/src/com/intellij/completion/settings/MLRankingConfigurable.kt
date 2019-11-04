// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.settings

import com.intellij.application.options.CodeCompletionOptionsCustomSection
import com.intellij.completion.StatsCollectorBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.layout.*

class MLRankingConfigurable(private val supportedLanguages: List<String>)
  : BoundConfigurable("ML Ranking") {
  private val settings = CompletionMLRankingSettings.getInstance()

  override fun createPanel(): DialogPanel {
    return panel {
      titledRow(StatsCollectorBundle.message("ml.completion.settings.group")) {
        row {
          val enableRanking = checkBox(StatsCollectorBundle.message("ml.completion.enable"), settings::isRankingEnabled,
                                       { settings.isRankingEnabled = it })
          for (language in supportedLanguages) {
            row {
              checkBox(language, { settings.isLanguageEnabled(language) }, { settings.setLanguageEnabled(language, it) })
                .enableIf(enableRanking.selected)
            }
          }
        }
        val registry = Registry.get("completion.stats.show.ml.ranking.diff")
        row {
          checkBox(StatsCollectorBundle.message("ml.completion.show.diff"),
                   { registry.asBoolean() },
                   { registry.setValue(it) })
        }
      }
    }
  }
}
