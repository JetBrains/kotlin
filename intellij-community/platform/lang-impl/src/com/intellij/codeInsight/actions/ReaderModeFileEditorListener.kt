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

    fun applyReaderMode(project: Project, selectedEditor: FileEditor?, fileIsOpenAlready: Boolean = false) {
      if (selectedEditor is PsiAwareTextEditorImpl) {
        val file = selectedEditor.file
        if (matchMode(project, file)) {
          EP_READER_MODE_PROVIDER.extensions().forEach {
            it.applyModeChanged(project, selectedEditor.editor, instance(project).enabled, fileIsOpenAlready)
          }
        }
      }
    }

    fun matchMode(project: Project?, file: VirtualFile?): Boolean {
      if (project == null || file == null) return false

      val inLibraries = FileIndexFacade.getInstance(project).isInLibraryClasses(file) || FileIndexFacade.getInstance(project).isInLibrarySource(file)
      val isWritable = file.isWritable

      return when (instance(project).mode) {
        ReaderMode.LIBRARIES_AND_READ_ONLY -> inLibraries || !isWritable
        ReaderMode.LIBRARIES -> inLibraries
        ReaderMode.READ_ONLY -> !isWritable
      }
    }
  }
}