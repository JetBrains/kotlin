// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.remote

import com.intellij.execution.remote.BaseExtendableConfiguration.Companion.getTypeImpl
import com.intellij.openapi.project.Project

abstract class RemoteTargetConfiguration(typeId: String)
  : BaseExtendableConfiguration(typeId, RemoteTargetType.EXTENSION_NAME) {

  val runtimes = BaseExtendableList(LanguageRuntimeType.EXTENSION_NAME)

  fun addLanguageRuntime(runtime: LanguageRuntimeConfiguration) = runtimes.addConfig(runtime)

  fun removeLanguageRuntime(runtime: LanguageRuntimeConfiguration) = runtimes.removeConfig(runtime)

  fun createRunner(project: Project): IR.RemoteRunner = getTargetType().createRunner(project, this)
}

fun <C : RemoteTargetConfiguration, T : RemoteTargetType<C>> C.getTargetType(): T = this.getTypeImpl()