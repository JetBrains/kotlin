// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.autoimport

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectAware
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectRefreshListener
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import java.io.File

class ProjectAware(
  private val project: Project,
  override val projectId: ExternalSystemProjectId,
  private val autoImportAware: ExternalSystemAutoImportAware
) : ExternalSystemProjectAware {

  private val LOG = Logger.getInstance("#com.intellij.openapi.externalSystem.autoimport")

  private val systemId = projectId.systemId
  private val projectPath = projectId.externalProjectPath

  override val settingsFiles: Set<String>
    get() = externalProjectFiles.map { FileUtil.toCanonicalPath(it.path) }.toSet()

  private val externalProjectFiles: List<File>
    get() = autoImportAware.getAffectedExternalProjectFiles(projectPath, project)

  override fun subscribe(listener: ExternalSystemProjectRefreshListener, parentDisposable: Disposable) {
    val progressManager = ServiceManager.getService(ExternalSystemProgressNotificationManager::class.java)
    val notificationListener = TaskNotificationListener(listener)
    progressManager.addNotificationListener(notificationListener)
    Disposer.register(parentDisposable, Disposable {
      progressManager.removeNotificationListener(notificationListener)
    })
  }

  override fun refreshProject() {
    LOG.debug("${projectId.readableName}: Refresh project")
    if (ExternalSystemUtil.isNoBackgroundMode()) return
    TransactionGuard.getInstance().submitTransactionLater(project, Runnable {
      ExternalSystemUtil.refreshProject(
        /*project =*/ project,
        /*externalSystemId =*/ systemId,
        /*externalProjectPath =*/ projectPath,
        /*isPreviewMode =*/ false,
        /*progressExecutionMode =*/ ProgressExecutionMode.IN_BACKGROUND_ASYNC
      )
    })
  }

  private inner class TaskNotificationListener(
    val delegate: ExternalSystemProjectRefreshListener
  ) : ExternalSystemTaskNotificationListenerAdapter() {
    var externalSystemTaskId: ExternalSystemTaskId? = null

    override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
      if (workingDir != projectPath) return
      externalSystemTaskId = id
      delegate.beforeProjectRefresh()
    }

    override fun onSuccess(id: ExternalSystemTaskId) {
      if (externalSystemTaskId == null) return
      externalSystemTaskId = null
      delegate.afterProjectRefresh(ExternalSystemRefreshStatus.SUCCESS)
    }

    override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
      if (externalSystemTaskId == null) return
      externalSystemTaskId = null
      delegate.afterProjectRefresh(ExternalSystemRefreshStatus.FAILURE)
    }

    override fun onCancel(id: ExternalSystemTaskId) {
      if (externalSystemTaskId == null) return
      externalSystemTaskId = null
      delegate.afterProjectRefresh(ExternalSystemRefreshStatus.CANCEL)
    }
  }
}