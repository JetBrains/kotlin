// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

class InlayHintsPassFactory : TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {
  override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
    registrar.registerTextEditorHighlightingPass(this, null, null, false, -1)
  }

  override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? = createPass(file, editor)


  companion object {
    private val INLAY_PSI_MODIFICATION_STAMP = Key.create<Long>("inlay.hints.stamp")

    fun forceHintsUpdateOnNextPass() {
      for (editor in EditorFactory.getInstance().allEditors) {
        editor.putUserData<Long>(INLAY_PSI_MODIFICATION_STAMP, null)
      }
      restartDaemon()
    }

    @JvmStatic
    fun restartDaemon() {
      ProjectManager.getInstance().openProjects.forEach { project ->
        val psiManager = PsiManager.getInstance(project)
        val daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(project)
        val fileEditorManager = FileEditorManager.getInstance(project)

        fileEditorManager.selectedFiles.forEach { file ->
          psiManager.findFile(file)?.let { daemonCodeAnalyzer.restart(it) }
        }
      }
    }

    fun createPass(file: PsiFile, editor: Editor, forceRefresh: Boolean = false): InlayHintsPass? {
      if (editor.isOneLineMode) return null
      val currentStamp = file.manager.modificationTracker.modificationCount
      val savedStamp = editor.getUserData<Long>(INLAY_PSI_MODIFICATION_STAMP)
      if (!forceRefresh && savedStamp != null && savedStamp == currentStamp) return null
      val settings = ServiceManager.getService(InlayHintsSettings::class.java)
      val language = file.language
      val collectors = HintUtils.getHintProvidersForLanguage(language, file.project)
        .mapNotNull { it.getCollectorWrapperFor(file, editor, language) }
      return InlayHintsPass(file, collectors, editor, settings)
    }
  }
}