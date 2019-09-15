// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.remote

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.util.containers.toArray

abstract class LanguageRuntimeType<C : LanguageRuntimeConfiguration>(id: String) : BaseExtendableType<C>(id) {

  abstract fun isApplicableTo(runConfig: RunnerAndConfigurationSettings): Boolean

  /**
   * Description of type's Configurable, e.g : "Configure GO"
   */
  abstract val configurableDescription: String

  companion object {
    val EXTENSION_NAME = ExtensionPointName.create<LanguageRuntimeType<*>>("com.intellij.ir.languageRuntime")
    @JvmStatic
    fun allTypes() = EXTENSION_NAME.extensionList.toArray(emptyArray<LanguageRuntimeType<*>>())
  }
}
