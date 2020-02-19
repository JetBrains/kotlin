// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport.changes

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ModificationType
import com.intellij.openapi.externalSystem.util.PathPrefixTreeMap
import com.intellij.util.EventDispatcher
import java.util.concurrent.Executor

abstract class AsyncFilesChangesProviderBase(private val backgroundExecutor: Executor) : AsyncFilesChangesProvider, Disposable {
  private val eventDispatcher = EventDispatcher.create(FilesChangesListener::class.java)

  override fun subscribe(listener: FilesChangesListener, parentDisposable: Disposable) {
    eventDispatcher.addListener(listener, parentDisposable)
  }

  private var updatedFiles = HashMap<String, ModificationData>()

  override fun init() {
    updatedFiles = HashMap()
  }

  override fun onFileChange(path: String, modificationStamp: Long, modificationType: ModificationType) {
    updatedFiles[path] = ModificationData(modificationStamp, modificationType)
  }

  override fun apply() {
    processUpdatedFiles(updatedFiles)
  }

  private fun processUpdatedFiles(updatedFiles: Map<String, ModificationData>) {
    submitFilesCollecting { filesToWatch ->
      val index = PathPrefixTreeMap<Boolean>()
      filesToWatch.forEach { index[it] = true }
      eventDispatcher.multicaster.init()
      for ((path, modificationData) in updatedFiles) {
        val (modificationStamp, modificationType) = modificationData
        for (relevantPath in index.getAllAncestorKeys(path)) {
          eventDispatcher.multicaster.onFileChange(relevantPath, modificationStamp, modificationType)
        }
      }
      eventDispatcher.multicaster.apply()
    }
  }

  private fun submitFilesCollecting(action: (Set<String>) -> Unit) {
    if (ApplicationManager.getApplication().isHeadlessEnvironment) {
      action(collectRelevantFiles())
      return
    }
    ReadAction.nonBlocking<Set<String>> { collectRelevantFiles() }
      .expireWith(this)
      .finishOnUiThread(ModalityState.defaultModalityState(), action)
      .submit(backgroundExecutor)
  }

  override fun dispose() {}

  private data class ModificationData(val modificationStamp: Long, val modificationType: ModificationType)
}