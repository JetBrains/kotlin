// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport.changes

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction

interface AsyncFilesChangesProvider : FilesChangesProvider, FilesChangesListener {

  /**
   * Collects relevant files, changes in that are be listened
   * [FilesChangesListener.onFileChange] will be called only for relevant files
   * This function is called on background thread in read action
   * Please use [com.intellij.openapi.progress.ProgressManager.checkCanceled] while collecting
   *
   * @see [ReadAction.nonBlocking]
   */
  fun collectRelevantFiles(): Set<String>

  /**
   * Subscribes to changes in files that are be defined by [collectRelevantFiles]
   * Functions of [listener] are called on UI thread
   *
   * @see [com.intellij.openapi.application.NonBlockingReadAction.finishOnUiThread]
   */
  override fun subscribe(listener: FilesChangesListener, parentDisposable: Disposable)
}