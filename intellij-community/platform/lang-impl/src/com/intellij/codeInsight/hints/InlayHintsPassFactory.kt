// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiFile

class InlayHintsPassFactory : TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {
  override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
    registrar.registerTextEditorHighlightingPass(this, null, null, false, -1)
  }

  override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
    if (editor.isOneLineMode) return null
    val settings = InlayHintsSettings.instance()
    val language = file.language
    val collectors = HintUtils.getHintProvidersForLanguage(language, file.project)
      .mapNotNull { it.getCollectorWrapperFor(file, editor, language) }
    return InlayHintsPass(file, collectors, editor, settings)
  }

  companion object {
    fun forceHintsUpdateOnNextPass() {
      ProjectManager.getInstance().openProjects.forEach { project ->
        DaemonCodeAnalyzer.getInstance(project).restart()
      }
    }
  }
}