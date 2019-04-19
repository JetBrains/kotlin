// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider

class CompletionStatsCollectorConfigurableProvider : ConfigurableProvider() {
  override fun createConfigurable(): Configurable? {
    return CompletionCollectorConfigurable()
  }
}