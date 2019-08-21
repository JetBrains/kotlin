// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.completion.ngram.slp.modeling.runners.ModelRunner
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.annotations.TestOnly

class NgramFileConfigurator(private val project: Project) : FileEditorManagerListener.Before {

  override fun beforeFileOpened(source: FileEditorManager, file: VirtualFile) {
    val psiFile = PsiUtilCore.getPsiFile(project, file)
    val language = psiFile.language
    if (isSupported(language)) {
      ApplicationManager.getApplication().executeOnPooledThread {
        val modelRunner = makeModelRunner()
        //modelRunner.setSelfTesting(true)
        DumbService.getInstance(project).runReadActionInSmartMode {
          modelRunner.learnPsiFile(psiFile)
        }
        file.putUserData(NGRAM_MODEL_KEY, modelRunner)
      }
    }
  }

  override fun beforeFileClosed(source: FileEditorManager, file: VirtualFile) {
    file.putUserData(NGRAM_MODEL_KEY, null)
  }

  companion object {
    private val NGRAM_MODEL_KEY: Key<ModelRunner> = Key.create("ngramModel")
    private val SUPPORTED_LANGUAGES = setOf(
      "ecmascript 6",
      "go",
      "java",
      "kotlin",
      "php",
      "python",
      "ruby",
      "scala",
      "shell script"
    )

    internal fun isSupported(language: Language): Boolean {
      return SUPPORTED_LANGUAGES.contains(language.id.toLowerCase())
    }

    fun getModelRunner(file: PsiFile): ModelRunner? {
      return file.virtualFile.getUserData(NGRAM_MODEL_KEY)
    }

    @TestOnly
    fun putModelRunner(file: PsiFile, modelRunner: ModelRunner) {
      file.virtualFile.putUserData(NGRAM_MODEL_KEY, modelRunner)
    }
  }

}