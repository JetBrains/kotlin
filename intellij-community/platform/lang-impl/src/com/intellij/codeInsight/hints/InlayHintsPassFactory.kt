// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile

class InlayHintsPassFactory : TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {
  override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
    registrar.registerTextEditorHighlightingPass(this, null, null, false, -1)
  }

  override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
    if (editor.isOneLineMode) return null
    val savedStamp = editor.getUserData(PSI_MODIFICATION_STAMP)
    val currentStamp = getCurrentModificationStamp(file)
    if (savedStamp != null && savedStamp == currentStamp) return null

    val settings = InlayHintsSettings.instance()
    val language = file.language
    val collectors = if (isHintsEnabledForEditor(editor)) {
      HintUtils.getHintProvidersForLanguage(language, file.project)
        .mapNotNull { it.getCollectorWrapperFor(file, editor, language) }
        .filter { settings.hintsShouldBeShown(it.key, language) }
    }
    else {
      emptyList()
    }
    return InlayHintsPass(file, collectors, editor)
  }

  companion object {
    fun forceHintsUpdateOnNextPass() {
      for (editor in EditorFactory.getInstance().allEditors) {
        editor.putUserData(PSI_MODIFICATION_STAMP, null)
      }
      ProjectManager.getInstance().openProjects.forEach { project ->
        DaemonCodeAnalyzer.getInstance(project).restart()
      }
    }

    @JvmStatic
    private val PSI_MODIFICATION_STAMP = Key.create<Long>("inlay.psi.modification.stamp")
    @JvmStatic
    private val HINTS_DISABLED_FOR_EDITOR = Key.create<Boolean>("inlay.hints.enabled.for.editor")

    fun putCurrentModificationStamp(editor: Editor, file: PsiFile) {
      editor.putUserData(PSI_MODIFICATION_STAMP, getCurrentModificationStamp(file))
    }

    private fun getCurrentModificationStamp(file: PsiFile): Long {
      return file.manager.modificationTracker.modificationCount
    }

    private fun isHintsEnabledForEditor(editor: Editor): Boolean {
      return editor.getUserData(HINTS_DISABLED_FOR_EDITOR) != true
    }

    @JvmStatic
    fun setHintsEnabled(editor: Editor, value: Boolean) {
      if (value) {
        editor.putUserData(HINTS_DISABLED_FOR_EDITOR, null)
      }
      else {
        editor.putUserData(HINTS_DISABLED_FOR_EDITOR, true)
      }
      forceHintsUpdateOnNextPass()
    }
  }
}