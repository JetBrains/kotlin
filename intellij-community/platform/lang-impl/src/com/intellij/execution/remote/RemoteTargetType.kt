// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.remote

import com.intellij.execution.ExecutionTarget
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

//TODO: suggest "predefined" configurations (e.g one per every configured SFTP connection)
abstract class RemoteTargetType<C : RemoteTargetConfiguration>(id: String) : BaseExtendableType<C>(id) {

  abstract fun createExecutionTarget(project: Project, config: C): ExecutionTarget?

  abstract fun createRunner(project: Project, config: C): IR.RemoteRunner

  companion object {
    @JvmStatic
    val EXTENSION_NAME = ExtensionPointName.create<RemoteTargetType<*>>("com.intellij.ir.targetType")
  }
}