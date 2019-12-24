// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.externalSystem.autoimport.NonBlockingReadActionBuilder.Companion.nonBlockingReadAction
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ModificationType
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ModificationType.EXTERNAL
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ModificationType.INTERNAL
import com.intellij.openapi.externalSystem.util.calculateCrc
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.observable.operations.AnonymousParallelOperationTrace
import com.intellij.openapi.observable.operations.CompoundParallelOperationTrace
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.ExternalChangeAction
import com.intellij.util.LocalTimeCounter.currentTime
import org.jetbrains.annotations.ApiStatus
import java.io.File
import java.util.concurrent.atomic.AtomicReference

@ApiStatus.Internal
class ProjectSettingsTracker(
  private val project: Project,
  private val projectTracker: AutoImportProjectTracker,
  private val projectAware: ExternalSystemProjectAware,
  private val parentDisposable: Disposable
) {

  private val LOG = Logger.getInstance("#com.intellij.openapi.externalSystem.autoimport")

  private val status = ProjectStatus(debugName = "Settings ${projectAware.projectId.readableName}")

  private val settingsFilesCRC = AtomicReference(emptyMap<String, Long>())

  private val applyChangesOperation = AnonymousParallelOperationTrace(debugName = "Apply changes operation")

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
          true -> status.markDirty(currentTime())
          else -> status.markReverted(currentTime())
        }
        projectTracker.scheduleProjectRefresh()
      }
    }
  }

  fun getState() = State(status.isDirty(), settingsFilesCRC.get().toMap())

  fun loadState(state: State) {
    if (state.isDirty) status.markDirty(currentTime())
    settingsFilesCRC.set(state.settingsFiles.toMap())
  }

  private fun isAsyncAllowed() = !ApplicationManager.getApplication().isHeadlessEnvironment

  private fun submitSettingsFilesRefresh(callback: () -> Unit = {}) {
    invokeLater {
      val fileDocumentManager = FileDocumentManager.getInstance()
      fileDocumentManager.saveAllDocuments()
      val localFileSystem = LocalFileSystem.getInstance()
      val settingsFiles = projectAware.settingsFiles.map { File(it) }
      localFileSystem.refreshIoFiles(settingsFiles, isAsyncAllowed(), false, callback)
    }
  }

  private fun submitSettingsFilesCRCCalculation(action: (Map<String, Long>) -> Unit) {
    nonBlockingReadAction { calculateSettingsFilesCRC() }
      .finishOnUiThread { action(it) }
      .submit(parentDisposable)
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
    val settingsListener = ProjectDocumentSettingsListener()
    val eventMulticaster = EditorFactory.getInstance().eventMulticaster
    eventMulticaster.addDocumentListener(settingsListener, parentDisposable)
  }

  init {
    val settingsListener = ProjectVirtualFileSettingsListener()
    val fileManager = VirtualFileManager.getInstance()
    fileManager.addAsyncFileListener(settingsListener, parentDisposable)
  }

  data class State(var isDirty: Boolean = true, var settingsFiles: Map<String, Long> = emptyMap())

  private inner class ProjectVirtualFileSettingsListener : AsyncFileChangeListenerBase() {
    private val delegate = ProjectSettingsListener()

    override fun isRelevant(path: String) = delegate.isRelevant(path)

    override fun updateFile(file: VirtualFile, event: VFileEvent) {
      if (event.isFromSave) return
      val modificationType = if (event.isFromRefresh) EXTERNAL else INTERNAL
      delegate.updateFile(file.path, file.modificationStamp, modificationType)
    }

    override fun init() = delegate.init()

    override fun apply() = delegate.apply()
  }

  private inner class ProjectDocumentSettingsListener : DocumentListener {
    private val bulkUpdateOperation = CompoundParallelOperationTrace<Document>(debugName = "Bulk document update operation")
    private val delegate = ProjectSettingsListener()

    private fun isExternalModification() =
      ApplicationManager.getApplication().hasWriteAction(ExternalChangeAction::class.java)

    override fun documentChanged(event: DocumentEvent) {
      if (isExternalModification()) return
      val document = event.document
      val fileDocumentManager = FileDocumentManager.getInstance()
      val file = fileDocumentManager.getFile(document) ?: return
      when (bulkUpdateOperation.isOperationCompleted()) {
        true -> {
          delegate.init()
          if (!delegate.isRelevant(file.path)) return
          delegate.updateFile(file.path, document.modificationStamp, INTERNAL)
          delegate.apply()
        }
        else -> {
          if (!delegate.isRelevant(file.path)) return
          delegate.updateFile(file.path, document.modificationStamp, INTERNAL)
        }
      }
    }

    override fun bulkUpdateStarting(document: Document) {
      bulkUpdateOperation.startTask(document)
    }

    override fun bulkUpdateFinished(document: Document) {
      bulkUpdateOperation.finishTask(document)
    }

    init {
      bulkUpdateOperation.beforeOperation { delegate.init() }
      bulkUpdateOperation.afterOperation { delegate.apply() }
    }
  }

  private inner class ProjectSettingsListener {
    @Volatile
    private var hasRelevantChanges = false

    @Volatile
    private var settingsFilesSnapshot: Set<String> = emptySet()

    fun isRelevant(path: String): Boolean {
      val isRelevant = path in settingsFilesSnapshot
      hasRelevantChanges = hasRelevantChanges || isRelevant
      return isRelevant
    }

    fun updateFile(path: String, modificationStamp: Long, type: ModificationType) {
      hasRelevantChanges = true
      logModificationAsDebug(path, modificationStamp, type)
      if (applyChangesOperation.isOperationCompleted()) {
        status.markModified(currentTime(), type)
      }
      else {
        status.markDirty(currentTime())
      }
    }

    fun init() {
      hasRelevantChanges = false
      settingsFilesSnapshot = projectAware.settingsFiles + settingsFilesCRC.get().keys
    }

    fun apply() {
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