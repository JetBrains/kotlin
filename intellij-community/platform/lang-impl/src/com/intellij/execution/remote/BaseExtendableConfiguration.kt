// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.remote

import com.intellij.openapi.extensions.ExtensionPointName

abstract class BaseExtendableConfiguration(val typeId: String, internal val extensionPoint: ExtensionPointName<out BaseExtendableType<*>>) {

  var displayName: String = ""

  companion object {
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    internal fun <C : BaseExtendableConfiguration, T : BaseExtendableType<C>> C.getTypeImpl(): T =
      this.extensionPoint.extensionList.find { it.id == typeId } as T?
      ?: throw IllegalStateException("for type: $typeId, name: $displayName")
  }
}