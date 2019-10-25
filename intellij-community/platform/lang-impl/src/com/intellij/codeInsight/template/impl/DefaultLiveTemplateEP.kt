// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl

import com.intellij.openapi.extensions.AbstractExtensionPointBean
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.util.xmlb.annotations.Attribute

/**
 * @author yole
 */
class DefaultLiveTemplateEP : AbstractExtensionPointBean() {
  @Attribute("file")
  @RequiredElement
  var file: String? = null

  @Attribute("hidden")
  var hidden: Boolean = false

  companion object {
    @JvmField
    val EP_NAME = ExtensionPointName.create<DefaultLiveTemplateEP>("com.intellij.defaultLiveTemplates")
  }
}
