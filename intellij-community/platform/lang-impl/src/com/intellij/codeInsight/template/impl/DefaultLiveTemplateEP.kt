// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl

import com.intellij.openapi.extensions.AbstractExtensionPointBean
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.util.xmlb.annotations.Attribute

/**
 * Provides bundled live templates.
 * See [Live Templates Tutorial](http://www.jetbrains.org/intellij/sdk/docs/tutorials/live_templates.html).
 *
 * @author yole
 */
class DefaultLiveTemplateEP : AbstractExtensionPointBean() {

  /**
   * Path to resource, without `.xml` extension (e.g. `/templates/foo`).
   */
  @Attribute("file")
  @RequiredElement
  var file: String? = null

  /**
   * `true` if not user-visible/editable.
   */
  @Attribute("hidden")
  var hidden: Boolean = false

  companion object {
    @JvmField
    val EP_NAME = ExtensionPointName.create<DefaultLiveTemplateEP>("com.intellij.defaultLiveTemplates")
  }
}
