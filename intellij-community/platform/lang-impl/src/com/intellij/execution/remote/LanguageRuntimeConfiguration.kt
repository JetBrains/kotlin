// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.remote

import com.intellij.execution.remote.BaseExtendableConfiguration.Companion.getTypeImpl

abstract class LanguageRuntimeConfiguration(typeId: String)
  : BaseExtendableConfiguration(typeId, LanguageRuntimeType.EXTENSION_NAME) {
}

fun <C : LanguageRuntimeConfiguration, T : LanguageRuntimeType<C>> C.getRuntimeType(): T = this.getTypeImpl()