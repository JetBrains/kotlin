// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.util.concurrency.SequentialTaskExecutor
import java.util.concurrent.ExecutorService

class NGramFileListener(private val project: Project) : FileEditorManagerListener.Before {
  override fun beforeFileOpened(source: FileEditorManager, file: VirtualFile) {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return
    val language = psiFile.language
    if (!NGram.isSupported(language)) return
    val filePointer = SmartPointerManager.createPointer(psiFile)
    ReadAction.nonBlocking(Runnable {
      NGramModelRunnerManager.getInstance(project).processFile(filePointer, language)
      SmartPointerManager.getInstance(project).removePointer(filePointer)
    }).inSmartMode(project).submit(executor)

  }

  private companion object {
    private val executor: ExecutorService = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("N-grams for recent files")
  }
}