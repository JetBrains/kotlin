// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ModificationType
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ModificationType.EXTERNAL
import com.intellij.openapi.externalSystem.autoimport.changes.AsyncFilesChangesProviderImpl
import com.intellij.openapi.externalSystem.autoimport.changes.FilesChangesListener
import com.intellij.openapi.externalSystem.util.calculateCrc
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.observable.operations.AnonymousParallelOperationTrace
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.LocalTimeCounter.currentTime
import org.jetbrains.annotations.ApiStatus
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

@ApiStatus.Internal
class ProjectSettingsTracker(
  private val project: Project,
  private val projectTracker: AutoImportProjectTracker,
  private val backgroundExecutor: Executor,
  private val projectAware: ExternalSystemProjectAware,
  private val parentDisposable: Disposable
) {

  private val LOG = Logger.getInstance("#com.intellij.openapi.externalSystem.autoimport")

  private val status = ProjectStatus(debugName = "Settings ${projectAware.projectId.readableName}")

  private val settingsFilesCRC = AtomicReference(emptyMap<String, Long>())

  private val applyChangesOperation = AnonymousParallelOperationTrace(debugName = "Apply changes operation")

  private fun collectSettingsFiles() = projectAware.settingsFiles + settingsFilesCRC.get().keys

  private fun calculateSettingsFilesCRC(): Map<String, Long> {
    val localFileSystem = LocalFileSystem.getInstance()
    return projectAware.settingsFiles
      .mapNotNull { localFileSystem.findFileByPath(it) }
      .map { it.path to calculateCrc(it) }
      .toMap()
  }

  private fun calculateCrc(file: VirtualFile): Long {
    val fileDocumentManager = FileDocumentManager.getInstance()
    val document = fileDocumentManager.getCachedDocument(file)
    if (document != null) return document.calculateCrc(project, file)
    return file.calculateCrc(project)
  }

  fun isUpToDate() = status.isUpToDate()

  fun getModificationType() = status.getModificationType()

  private fun hasChanges(newSettingsFilesCRC: Map<String, Long>): Boolean {
    val oldSettingsFilesCRC = settingsFilesCRC.get()
    if (newSettingsFilesCRC.size != oldSettingsFilesCRC.size) return true
    return newSettingsFilesCRC.any { it.value != oldSettingsFilesCRC[it.key] }
  }

  /**
   * Usually all crc hashes must be previously calculated
   *  => this apply will be fast
   *  => collisions is a rare thing
   */
  private fun applyChanges() {
    applyChangesOperation.startTask()
    submitSettingsFilesRefresh {
      submitSettingsFilesCRCCalculation { newSettingsFilesCRC ->
        settingsFilesCRC.set(newSettingsFilesCRC)
        status.markSynchronized(currentTime())
        applyChangesOperation.finishTask()
      }
    }
  }

  /**
   * Applies changes for newly registered files
   * Needed to cases: tracked files are registered during project reload
   */
  private fun applyUnknownChanges() {
    applyChangesOperation.startTask()
    submitSettingsFilesRefresh {
      submitSettingsFilesCRCCalculation { newSettingsFilesCRC ->
        settingsFilesCRC.updateAndGet { newSettingsFilesCRC + it }
        if (!hasChanges(newSettingsFilesCRC)) {
          status.markSynchronized(currentTime())
        }
        applyChangesOperation.finishTask()
      }
    }
  }

  fun refreshChanges() {
    submitSettingsFilesRefresh {
      submitSettingsFilesCRCCalculation { newSettingsFilesCRC ->
        when (hasChanges(newSettingsFilesCRC)) {
          true -> status.markDirty(currentTime(), EXTERNAL)
          else -> status.markReverted(currentTime())
        }
        projectTracker.scheduleChangeProcessing()
      }
    }
  }

  fun getState() = State(status.isDirty(), settingsFilesCRC.get().toMap())

  fun loadState(state: State) {
    if (state.isDirty) status.markDirty(currentTime(), EXTERNAL)
    settingsFilesCRC.set(state.settingsFiles.toMap())
  }

  private fun isAsyncAllowed() = projectTracker.isAsyncChangesProcessing

  private fun submitSettingsFilesRefresh(callback: () -> Unit = {}) {
    invokeLater {
      val fileDocumentManager = FileDocumentManager.getInstance()
      fileDocumentManager.saveAllDocuments()
      submitSettingsFilesCollection { settingsPaths ->
        val localFileSystem = LocalFileSystem.getInstance()
        val settingsFiles = settingsPaths.map { File(it) }
        localFileSystem.refreshIoFiles(settingsFiles, isAsyncAllowed(), false, callback)
      }
    }
  }

  private fun submitSettingsFilesCollection(action: (Set<String>) -> Unit) {
    if (!isAsyncAllowed()) {
      action(collectSettingsFiles())
      return
    }
    ReadAction.nonBlocking<Set<String>> { collectSettingsFiles() }
      .expireWith(parentDisposable)
      .finishOnUiThread(ModalityState.defaultModalityState(), action)
      .submit(backgroundExecutor)
  }

  private fun submitSettingsFilesCRCCalculation(action: (Map<String, Long>) -> Unit) {
    if (!isAsyncAllowed()) {
      action(calculateSettingsFilesCRC())
      return
    }
    ReadAction.nonBlocking<Map<String, Long>> { calculateSettingsFilesCRC() }
      .expireWith(parentDisposable)
      .finishOnUiThread(ModalityState.defaultModalityState(), action)
      .submit(backgroundExecutor)
  }

  private fun invokeLater(action: () -> Unit) {
    val application = ApplicationManager.getApplication()
    if (!isAsyncAllowed()) {
      application.invokeAndWait(action)
    }
    else {
      application.invokeLater(action, { Disposer.isDisposed(parentDisposable) })
    }
  }

  fun beforeApplyChanges(listener: () -> Unit) = applyChangesOperation.beforeOperation(listener)
  fun afterApplyChanges(listener: () -> Unit) = applyChangesOperation.afterOperation(listener)

  init {
    val projectRefreshListener = object : ExternalSystemProjectRefreshListener {
      override fun beforeProjectRefresh() {
        applyChangesOperation.startTask()
        applyChanges()
      }

      override fun afterProjectRefresh(status: ExternalSystemRefreshStatus) {
        applyUnknownChanges()
        applyChangesOperation.finishTask()
      }
    }
    projectAware.subscribe(projectRefreshListener, parentDisposable)
  }

  init {
    AsyncFilesChangesProviderImpl(backgroundExecutor, ::collectSettingsFiles)
      .subscribe(ProjectSettingsListener(), parentDisposable)
  }

  data class State(var isDirty: Boolean = true, var settingsFiles: Map<String, Long> = emptyMap())

  private inner class ProjectSettingsListener : FilesChangesListener {
    private var hasRelevantChanges = false

    override fun onFileChange(path: String, modificationStamp: Long, modificationType: ModificationType) {
      hasRelevantChanges = true
      logModificationAsDebug(path, modificationStamp, modificationType)
      if (applyChangesOperation.isOperationCompleted()) {
        status.markModified(currentTime(), modificationType)
      }
      else {
        status.markDirty(currentTime(), modificationType)
      }
    }

    override fun init() {
      hasRelevantChanges = false
    }

    override fun apply() {
      if (hasRelevantChanges) {
        submitSettingsFilesCRCCalculation { newSettingsFilesCRC ->
          if (!hasChanges(newSettingsFilesCRC)) {
            status.markReverted(currentTime())
          }
          projectTracker.scheduleChangeProcessing()
        }
      }
    }

    private fun logModificationAsDebug(path: String, modificationStamp: Long, type: ModificationType) {
      if (LOG.isDebugEnabled) {
        val projectPath = projectAware.projectId.externalProjectPath
        val relativePath = FileUtil.getRelativePath(projectPath, path, '/') ?: path
        LOG.debug("File $relativePath is modified at ${modificationStamp} as $type")
      }
    }
  }
}