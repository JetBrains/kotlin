// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.actions

import com.intellij.codeInsight.actions.ReaderModeSettings.Companion.instance
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.vfs.VirtualFile

class ReaderModeFileEditorListener : FileEditorManagerListener {
  override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
    applyReaderMode(source.project, source.getSelectedEditor(file))
  }

  companion object {
    private var EP_READER_MODE_PROVIDER = ExtensionPointName<ReaderModeProvider>("com.intellij.readerModeProvider")

    fun applyReaderMode(project: Project, selectedEditor: FileEditor?) {
      if (selectedEditor !is PsiAwareTextEditorImpl) return

      val file = selectedEditor.file
      if (!matchMode(project, file)) return
      EP_READER_MODE_PROVIDER.extensions().forEach { it.applyModeChanged(project, selectedEditor.editor, instance(project).enabled) }
    }

    fun matchMode(project: Project?, file: VirtualFile?): Boolean {
      return if (project == null || file == null) { false }
      else when (instance(project).mode) {
        ReaderMode.LIBRARIES ->
          FileIndexFacade.getInstance(project).isInLibraryClasses(file) || FileIndexFacade.getInstance(project).isInLibrarySource(file)
        ReaderMode.READ_ONLY -> !file.isWritable
      }
    }
  }
}