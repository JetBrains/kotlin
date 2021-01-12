// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.KeyedExtensionCollector
import org.jetbrains.annotations.ApiStatus

/**
 * Describes additional steps to configure or cleanup execution context
 * @see com.intellij.openapi.externalSystem.model.task.ExternalSystemTask for details
 */
@ApiStatus.Experimental
interface ExternalSystemExecutionAware {

  /**
   * Prepares execution context to execution
   * This method called after execution start but before
   * [com.intellij.openapi.externalSystem.model.task.ExternalSystemTask.execute]
   */
  fun prepareExecution(
    task: ExternalSystemTask,
    externalProjectPath: String,
    isPreviewMode: Boolean,
    taskNotificationListener: ExternalSystemTaskNotificationListener,
    project: Project
  )

  companion object {

    private val EP_COLLECTOR = KeyedExtensionCollector<ExternalSystemExecutionAware, ProjectSystemId>("com.intellij.externalExecutionAware")

    @JvmStatic
    fun getExtensions(systemId: ProjectSystemId): List<ExternalSystemExecutionAware> {
      return EP_COLLECTOR.forKey(systemId)
    }
  }
}