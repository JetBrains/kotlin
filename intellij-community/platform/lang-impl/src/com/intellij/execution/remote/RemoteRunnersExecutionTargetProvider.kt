// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.remote

import com.intellij.execution.ExecutionTarget
import com.intellij.execution.ExecutionTargetProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class RemoteRunnersExecutionTargetProvider : ExecutionTargetProvider() {

  override fun getTargets(project: Project, configuration: RunConfiguration): List<ExecutionTarget> {
    return RemoteTargetsManager.instance.targets.resolvedConfigs()
      .map { it.getTargetType().createExecutionTarget(project, it) }
      .filterNotNull()
  }
}