// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions

import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.CopyReferenceUtil.getElementsToCopy
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.ui.tabs.impl.TabLabel
import java.awt.datatransfer.StringSelection

abstract class CopyPathProvider : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    if (!CopyPathsAction.isCopyReferencePopupAvailable()) {
      e.presentation.isEnabledAndVisible = false
      return
    }

    val dataContext = e.dataContext
    val editor = CommonDataKeys.EDITOR.getData(dataContext)
    val project = e.project
    e.presentation.isEnabledAndVisible = project != null
                                         && getQualifiedName(project, getElementsToCopy(editor, dataContext), editor, dataContext) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = getEventProject(e)
    val dataContext = e.dataContext
    val editor = CommonDataKeys.EDITOR.getData(dataContext)

    val customDataContext = createCustomDataContext(dataContext)
    val elements = getElementsToCopy(editor, customDataContext)
    project?.let {
      val copy = getQualifiedName(project, elements, editor, customDataContext)
      CopyPasteManager.getInstance().setContents(StringSelection(copy))
      CopyReferenceUtil.setStatusBarText(project, IdeBundle.message("message.path.to.fqn.has.been.copied", copy))

      CopyReferenceUtil.highlight(editor, project, elements)
    }
  }

  private fun createCustomDataContext(dataContext: DataContext): DataContext {
    val component = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext)
    if (component !is TabLabel) return dataContext

    val file = component.info.`object`
    if (file !is VirtualFile) return dataContext

    return SimpleDataContext.getSimpleContext(
      mapOf(LangDataKeys.VIRTUAL_FILE.name to file, CommonDataKeys.VIRTUAL_FILE_ARRAY.name to arrayOf(file)),
      dataContext)
  }

  open fun getQualifiedName(project: Project, elements: List<PsiElement>, editor: Editor?, dataContext: DataContext): String? {
    if (elements.isEmpty()) {
      return getPathToElement(project, editor?.document?.let { FileDocumentManager.getInstance().getFile(it) }, editor)
    }

    val refs =
      elements
        .mapNotNull { getPathToElement(project, (if (it is PsiFileSystemItem) it.virtualFile else it.containingFile?.virtualFile), editor) }
        .ifEmpty { CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)?.mapNotNull { getPathToElement(project, it, editor) } }
        .orEmpty()
        .filter { !it.isBlank() }

    return if (refs.isNotEmpty()) refs.joinToString("\n") else null
  }

  open fun getPathToElement(project: Project, virtualFile: VirtualFile?, editor: Editor?): String? = null
}

class CopyAbsolutePathProvider : CopyPathProvider() {
  override fun getPathToElement(project: Project, virtualFile: VirtualFile?, editor: Editor?) = virtualFile?.presentableUrl
}

class CopyContentRootPathProvider : CopyPathProvider() {
  override fun getPathToElement(project: Project,
                                virtualFile: VirtualFile?,
                                editor: Editor?): String? {
    return virtualFile?.let {
      ProjectFileIndex.getInstance(project).getModuleForFile(virtualFile, false)?.let { module ->
        ModuleRootManager.getInstance(module).contentRoots.mapNotNull { root ->
          VfsUtilCore.getRelativePath(virtualFile, root)
        }.singleOrNull()
      }
    }
  }
}

class CopyFileWithLineNumberPathProvider : CopyPathProvider() {
  override fun getPathToElement(project: Project,
                                virtualFile: VirtualFile?,
                                editor: Editor?): String? {
    return if (virtualFile == null) null
    else editor?.let { CopyReferenceUtil.getVirtualFileFqn(virtualFile, project) + ":" + (editor.caretModel.logicalPosition.line + 1) }
  }
}

class CopySourceRootPathProvider : CopyPathProvider() {
  override fun getPathToElement(project: Project, virtualFile: VirtualFile?, editor: Editor?) =
    virtualFile?.let {
      VfsUtilCore.getRelativePath(virtualFile, ProjectFileIndex.getInstance(project).getSourceRootForFile(virtualFile) ?: return null)
    }
}

class CopyTBXReferenceProvider : CopyPathProvider() {
  override fun getQualifiedName(project: Project,
                                elements: List<PsiElement>,
                                editor: Editor?,
                                dataContext: DataContext): String? =
    CopyTBXReferenceAction.createJetBrainsLink(project, elements, editor)
}

class CopyFileNameProvider : CopyPathProvider() {
  override fun getPathToElement(project: Project, virtualFile: VirtualFile?, editor: Editor?): String? = virtualFile?.nameWithoutExtension
}