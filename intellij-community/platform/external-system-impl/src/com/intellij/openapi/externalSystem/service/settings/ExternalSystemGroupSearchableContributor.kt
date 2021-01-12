// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.settings

import com.intellij.ide.ui.search.SearchableOptionContributor
import com.intellij.ide.ui.search.SearchableOptionProcessor
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle.message

class ExternalSystemGroupSearchableContributor: SearchableOptionContributor() {

  private val buildToolsId = "build.tools"
  private val buildToolsName = message("settings.build.tools.display.name")

  override fun processOptions(processor: SearchableOptionProcessor) {
    processor.addOptions(
      "reload autoreload auto-reload import autoimport auto-import",
      null,
      message("settings.build.tools.auto.reload.hit"),
      buildToolsId,
      buildToolsName,
      false)
  }

}