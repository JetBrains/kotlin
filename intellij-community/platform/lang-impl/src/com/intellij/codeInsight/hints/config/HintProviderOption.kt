// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.config

import com.intellij.codeInsight.hints.SettingsKey


internal data class HintProviderOption<T>(
  val key: SettingsKey<T>,
  val name: String,
  private var isEnabled: Boolean,
  val previewText: String?
) {

  fun setEnabled(enabled: Boolean) {
    isEnabled = enabled
  }

  fun isEnabled(): Boolean {
    return isEnabled
  }
}