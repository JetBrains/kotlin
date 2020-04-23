// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

class NGramFileListener(private val project: Project) : FileEditorManagerListener.Before {

  override fun beforeFileOpened(source: FileEditorManager, file: VirtualFile) {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return
    val language = psiFile.language
    if (NGram.isSupported(language)) {
      ApplicationManager.getApplication().executeOnPooledThread {
        DumbService.getInstance(project).runReadActionInSmartMode {
          ServiceManager.getService(project, NGramModelRunnerManager::class.java).processFile(psiFile, language)
        }
      }
    }
  }

}