// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport.changes

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import java.util.concurrent.Executor

class AsyncFilesChangesProviderImpl(
  private val backgroundExecutor: Executor,
  private val collectRelevantFiles: () -> Set<String>
) : FilesChangesProvider {
  override fun subscribe(listener: FilesChangesListener, parentDisposable: Disposable) {
    subscribeAsAsyncVirtualFilesChangesProvider(true, listener, parentDisposable)
    subscribeAsAsyncDocumentChangesProvider(listener, parentDisposable)
  }

  fun subscribeAsAsyncVirtualFilesChangesProvider(
    isIgnoreUpdatesFromSave: Boolean,
    listener: FilesChangesListener,
    parentDisposable: Disposable
  ) {
    val changesProvider = VirtualFilesChangesProvider(isIgnoreUpdatesFromSave)
    val fileManager = VirtualFileManager.getInstance()
    fileManager.addAsyncFileListener(changesProvider, parentDisposable)

    changesProvider.subscribeAsAsync(listener, parentDisposable)
  }

  private fun subscribeAsAsyncDocumentChangesProvider(listener: FilesChangesListener, parentDisposable: Disposable) {
    val changesProvider = DocumentsChangesProvider()
    val eventMulticaster = EditorFactory.getInstance().eventMulticaster
    eventMulticaster.addDocumentListener(changesProvider, parentDisposable)

    changesProvider.subscribeAsAsync(listener, parentDisposable)
  }

  private fun FilesChangesProvider.subscribeAsAsync(listener: FilesChangesListener, parentDisposable: Disposable) {
    val asyncFilesChangesProvider = buildProvider()
    Disposer.register(parentDisposable, asyncFilesChangesProvider)
    asyncFilesChangesProvider.subscribe(listener, parentDisposable)

    subscribe(asyncFilesChangesProvider, parentDisposable)
  }

  private fun buildProvider(): AsyncFilesChangesProviderBase {
    return object : AsyncFilesChangesProviderBase(backgroundExecutor) {
      override fun collectRelevantFiles() = this@AsyncFilesChangesProviderImpl.collectRelevantFiles()
    }
  }
}