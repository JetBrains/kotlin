// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.autoimport

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware
import com.intellij.openapi.externalSystem.autoimport.*
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType.RESOLVE_PROJECT
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemProcessingManager
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemResolveProjectTask
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class ProjectAware(
  private val project: Project,
  override val projectId: ExternalSystemProjectId,
  private val autoImportAware: ExternalSystemAutoImportAware
) : ExternalSystemProjectAware {

  private val systemId = projectId.systemId
  private val projectPath = projectId.externalProjectPath

  override val settingsFiles: Set<String>
    get() = externalProjectFiles.map { FileUtil.toCanonicalPath(it.path) }.toSet()

  private val externalProjectFiles: List<File>
    get() = autoImportAware.getAffectedExternalProjectFiles(projectPath, project)

  override fun subscribe(listener: ExternalSystemProjectRefreshListener, parentDisposable: Disposable) {
    val progressManager = ExternalSystemProgressNotificationManager.getInstance()
    progressManager.addNotificationListener(TaskNotificationListener(listener), parentDisposable)
  }

  override fun reloadProject(context: ExternalSystemProjectReloadContext) {
    val importSpec = ImportSpecBuilder(project, systemId)
    if (!context.isExplicitReload) {
      importSpec.dontReportRefreshErrors()
    }
    ExternalSystemUtil.refreshProject(projectPath, importSpec)
  }

  private inner class TaskNotificationListener(
    val delegate: ExternalSystemProjectRefreshListener
  ) : ExternalSystemTaskNotificationListenerAdapter() {
    var externalSystemTaskId = AtomicReference<ExternalSystemTaskId?>(null)

    override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
      if (id.type != RESOLVE_PROJECT) return
      if (!FileUtil.pathsEqual(workingDir, projectPath)) return

      val task = ServiceManager.getService(ExternalSystemProcessingManager::class.java).findTask(id)
      if (task is ExternalSystemResolveProjectTask) {
        if (!autoImportAware.isApplicable(task.resolverPolicy)) return
      }
      externalSystemTaskId.set(id)
      delegate.beforeProjectRefresh()
    }

    private fun afterProjectRefresh(id: ExternalSystemTaskId, status: ExternalSystemRefreshStatus) {
      if (id.type != RESOLVE_PROJECT) return
      if (!externalSystemTaskId.compareAndSet(id, null)) return
      delegate.afterProjectRefresh(status)
    }

    override fun onSuccess(id: ExternalSystemTaskId) {
      afterProjectRefresh(id, ExternalSystemRefreshStatus.SUCCESS)
    }

    override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
      afterProjectRefresh(id, ExternalSystemRefreshStatus.FAILURE)
    }

    override fun onCancel(id: ExternalSystemTaskId) {
      afterProjectRefresh(id, ExternalSystemRefreshStatus.CANCEL)
    }
  }
}