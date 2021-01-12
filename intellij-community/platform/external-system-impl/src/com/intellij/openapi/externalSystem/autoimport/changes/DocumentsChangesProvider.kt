// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport.changes

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.observable.operations.CompoundParallelOperationTrace
import com.intellij.psi.ExternalChangeAction
import com.intellij.util.EventDispatcher

class DocumentsChangesProvider : FilesChangesProvider, DocumentListener {
  private val eventDispatcher = EventDispatcher.create(FilesChangesListener::class.java)

  override fun subscribe(listener: FilesChangesListener, parentDisposable: Disposable) {
    eventDispatcher.addListener(listener, parentDisposable)
  }

  private val bulkUpdateOperation = CompoundParallelOperationTrace<Document>(debugName = "Bulk document update operation")

  private fun isExternalModification() =
    ApplicationManager.getApplication().hasWriteAction(ExternalChangeAction::class.java)

  override fun documentChanged(event: DocumentEvent) {
    if (isExternalModification()) return
    val document = event.document
    val fileDocumentManager = FileDocumentManager.getInstance()
    val file = fileDocumentManager.getFile(document) ?: return
    when (bulkUpdateOperation.isOperationCompleted()) {
      true -> {
        eventDispatcher.multicaster.init()
        eventDispatcher.multicaster.onFileChange(file.path, document.modificationStamp, ProjectStatus.ModificationType.INTERNAL)
        eventDispatcher.multicaster.apply()
      }
      else -> {
        eventDispatcher.multicaster.onFileChange(file.path, document.modificationStamp, ProjectStatus.ModificationType.INTERNAL)
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
    bulkUpdateOperation.beforeOperation { eventDispatcher.multicaster.init() }
    bulkUpdateOperation.afterOperation { eventDispatcher.multicaster.apply() }
  }
}