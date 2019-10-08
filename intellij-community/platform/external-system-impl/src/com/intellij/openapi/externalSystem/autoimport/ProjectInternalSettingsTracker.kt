// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.externalSystem.util.CompoundParallelOperationTrace
import com.intellij.openapi.externalSystem.util.calculateCrc
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.ExternalChangeAction


internal class ProjectInternalSettingsTracker(
  private val project: Project,
  projectTracker: ProjectTracker,
  private val projectAware: ExternalSystemProjectAware,
  parentDisposable: Disposable
) : ProjectSettingsTracker(projectTracker, projectAware, parentDisposable) {

  override fun calculateSettingsFilesCRC(): Map<String, Long> {
    val localFileSystem = LocalFileSystem.getInstance()
    val fileDocumentManager = FileDocumentManager.getInstance()
    return projectAware.settingsFiles
      .mapNotNull { localFileSystem.findFileByPath(it) }
      .mapNotNull {
        fileDocumentManager.getDocument(it)
          ?.let { document -> it to document }
      }
      .map { it.first.path to it.second.calculateCrc(project, it.first) }
      .toMap()
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

  private inner class ProjectVirtualFileSettingsListener : AsyncFileChangeListenerBase() {
    private val delegate = ProjectSettingsListener()

    override fun isRelevant(path: String) = delegate.isRelevant(path)

    override fun updateFile(file: VirtualFile, event: VFileEvent) {
      if (event.isFromRefresh || event.isFromSave) return
      delegate.updateFile(file.path, file.modificationStamp)
    }

    override fun init() = delegate.init()

    override fun apply() = delegate.apply()
  }

  private inner class ProjectDocumentSettingsListener : DocumentListener {
    private val bulkUpdateOperation = CompoundParallelOperationTrace<Document>()
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
          delegate.updateFile(file.path, document.modificationStamp)
          delegate.apply()
        }
        else -> {
          if (!delegate.isRelevant(file.path)) return
          delegate.updateFile(file.path, document.modificationStamp)
        }
      }
    }

    override fun bulkUpdateStarting(document: Document) {
      bulkUpdateOperation.startOperation()
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
}