// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.util.calculateCrc
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

internal class ProjectExternalSettingsTracker(
  private val project: Project,
  projectTracker: ProjectTracker,
  private val projectAware: ExternalSystemProjectAware,
  parentDisposable: Disposable
) : ProjectSettingsTracker(projectTracker, projectAware, parentDisposable) {

  override fun calculateSettingsFilesCRC(): Map<String, Long> {
    val localFileSystem = LocalFileSystem.getInstance()
    return projectAware.settingsFiles
      .mapNotNull { localFileSystem.findFileByPath(it) }
      .map { it.path to it.calculateCrc(project) }
      .toMap()
  }

  init {
    val settingsListener = ProjectVirtualFileSettingsListener()
    val fileManager = VirtualFileManager.getInstance()
    fileManager.addAsyncFileListener(settingsListener, parentDisposable)
  }

  private inner class ProjectVirtualFileSettingsListener : AsyncFileChangeListenerBase() {
    private val delegate = ProjectSettingsListener()

    override fun isRelevant(path: String) = delegate.isRelevant(path)

    override fun updateFile(file: VirtualFile, event: VFileEvent) {
      if (!event.isFromRefresh) return
      delegate.updateFile(file.path, file.modificationStamp)
    }

    override fun init() = delegate.init()

    override fun apply() = delegate.apply()
  }
}