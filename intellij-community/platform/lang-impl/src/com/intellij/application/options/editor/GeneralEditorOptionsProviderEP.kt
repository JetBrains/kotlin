// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.ConfigurableEP
import com.intellij.openapi.options.UnnamedConfigurable

/**
 * Allow to provide additional 'Editor | General' options
 */
class GeneralEditorOptionsProviderEP : ConfigurableEP<UnnamedConfigurable>() {
  companion object {
    val EP_NAME = ExtensionPointName.create<GeneralEditorOptionsProviderEP>("com.intellij.generalEditorOptionsExtension")
  }
}