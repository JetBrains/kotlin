/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.execution.impl

import com.intellij.execution.IS_RUN_MANAGER_INITIALIZED
import com.intellij.execution.RunManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.impl.ProjectLifecycleListener

internal class ProjectRunConfigurationInitializer(project: Project) {
  init {
    val connection = project.messageBus.connect()
    connection.subscribe(ProjectLifecycleListener.TOPIC, object : ProjectLifecycleListener {
      override fun projectComponentsInitialized(eventProject: Project) {
        if (project === eventProject) {
          requestLoadWorkspaceAndProjectRunConfiguration(project)
        }
      }
    })
  }

  private fun requestLoadWorkspaceAndProjectRunConfiguration(project: Project) {
    if (IS_RUN_MANAGER_INITIALIZED.get(project) == true) {
      return
    }

    IS_RUN_MANAGER_INITIALIZED.set(project, true)
    // we must not fire beginUpdate here, because message bus will fire queued parent message bus messages (and, so, SOE may occur because all other projectOpened will be processed before us)
    // simply, you should not listen changes until project opened
    project.service<RunManager>()
  }
}