// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.completion.ngram.slp.modeling.runners.ModelRunner
import com.intellij.lang.Language
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class NGramModelRunnerManager {

  private val myModelRunners: MutableMap<Language, ModelRunnerWithCache> = mutableMapOf()

  fun processFile(file: PsiFile, language: Language) {
    myModelRunners.getOrPut(language, { ModelRunnerWithCache() }).processFile(file)
  }

  fun getModelRunnerForLanguage(language: Language): ModelRunner? {
    return myModelRunners[language]
  }

  companion object {
    fun getInstance(project: Project): NGramModelRunnerManager {
      return ServiceManager.getService(project, NGramModelRunnerManager::class.java)
    }
  }
}