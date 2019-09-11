// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.project.autoimport.AsyncFileChangeListenerBase
import com.intellij.openapi.externalSystem.service.project.autoimport.ConfigurationFileCrcFactory
import com.intellij.openapi.externalSystem.service.project.autoimport.ProjectStatus
import com.intellij.openapi.externalSystem.util.properties.Property.Companion.property
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.LocalTimeCounter.currentTime
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update

class ProjectSettingsTracker(
  private val project: Project,
  private val projectStatus: ProjectStatus,
  private val projectAware: ExternalSystemProjectAware,
  parentDisposable: Disposable
) : AsyncFileChangeListenerBase() {

  private val LOG = Logger.getInstance("#com.intellij.openapi.externalSystem.autoimport")

  private val changesMutex = Any()
  private var settingsFiles: Map<String, Long> = LinkedHashMap()

  private val hasRelevantChanges = property { false }
  private val settingsFilesLatch = property { projectAware.settingsFiles + settingsFiles.keys }
  private val dispatcher = MergingUpdateQueue("project tracker", 0, true, MergingUpdateQueue.ANY_COMPONENT, parentDisposable, null, false)

  override fun isRelevant(path: String) = path in settingsFilesLatch.get()

  override fun prepareFileDeletion(file: VirtualFile) {
    hasRelevantChanges.set(true)
    logModificationAsDebug(file)
    projectStatus.markModified(currentTime())
  }

  override fun updateFile(file: VirtualFile, event: VFileEvent) {
    hasRelevantChanges.set(true)
    logModificationAsDebug(file)
    projectStatus.markModified(currentTime())
  }

  override fun reset() {
    hasRelevantChanges.reset()
    settingsFilesLatch.reset()
  }

  override fun afterVfsChange() {
    if (hasRelevantChanges.get()) {
      scheduleProjectNotificationUpdate()
    }
    reset()
  }

  fun hasChanges() = synchronized(changesMutex) {
    val settingsFiles = getSettingsFiles()
    val intersect = settingsFiles.map { it.path }.intersect(this.settingsFiles.keys)
    if (intersect.size != this.settingsFiles.size) return true
    if (intersect.size != settingsFiles.size) return true
    settingsFiles.any { it.calculateCrc() != this.settingsFiles[it.path] }
  }

  private fun scheduleProjectNotificationUpdate() = dispatcher.queue(object : Update("notification") {
    override fun run() {
      if (!hasChanges()) projectStatus.markReverted(currentTime())
      val projectTracker = ExternalSystemProjectTracker.getInstance(project)
      projectTracker.scheduleProjectNotificationUpdate()
    }
  })

  fun applyChanges() = synchronized(changesMutex) {
    settingsFiles = getSettingsFiles()
      .map { it.path to it.calculateCrc() }
      .toMap()
  }

  fun getState() = synchronized(changesMutex) {
    State(settingsFiles.toMap())
  }

  fun loadState(state: State) = synchronized(changesMutex) {
    settingsFiles = state.settingsFiles.toMap()
  }

  private fun VirtualFile.calculateCrc(): Long {
    return ConfigurationFileCrcFactory(this).create()
  }

  private fun getSettingsFiles(): Set<VirtualFile> {
    val localFileSystem = LocalFileSystem.getInstance()
    return projectAware.settingsFiles
      .mapNotNull { localFileSystem.refreshAndFindFileByPath(it) }
      .toSet()
  }

  private fun logModificationAsDebug(file: VirtualFile) {
    if (LOG.isDebugEnabled) {
      val projectPath = projectAware.projectId.externalProjectPath
      val relativePath = FileUtil.getRelativePath(projectPath, file.path, '/')
      LOG.debug("File $relativePath is modified at ${file.modificationStamp}")
    }
  }

  data class State(var settingsFiles: Map<String, Long> = emptyMap())
}