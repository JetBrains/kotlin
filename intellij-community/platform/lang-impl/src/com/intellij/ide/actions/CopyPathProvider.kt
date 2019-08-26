// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions

import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.CopyReferenceUtil.getElementsToCopy
import com.intellij.ide.actions.CopyReferenceUtil.setStatusBarText
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

abstract class CopyPathProvider : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    if (!CopyPathsAction.isCopyReferencePopupAvailable()) {
      e.presentation.isEnabledAndVisible = false
      return
    }

    val dataContext = e.dataContext
    val editor = CommonDataKeys.EDITOR.getData(dataContext)

    val elementsToCopy = getElementsToCopy(editor, dataContext)
    val project = getEventProject(e)
    e.presentation.isEnabledAndVisible = if (project == null) false else getQualifiedName(project, elementsToCopy, editor).isNotEmpty()
    e.presentation.putClientProperty(CopyReferencePopup.COPY_REFERENCE_KEY,
                                     getQualifiedName(project, getElementsToCopy(editor, dataContext), editor))
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = getEventProject(e)
    val dataContext = e.dataContext
    val editor = CommonDataKeys.EDITOR.getData(dataContext)

    val copy = project?.let { getQualifiedName(it, getElementsToCopy(editor, dataContext), editor) }

    CopyPasteManager.getInstance().setContents(CopyReferenceFQNTransferable(copy))
    setStatusBarText(project, IdeBundle.message("message.path.to.fqn.has.been.copied", copy))
  }

  open fun getQualifiedName(project: Project?, elements: List<PsiElement>, editor: Editor?): String {
    return elements.map { element ->
      project?.let { getPathToElement(it, element.containingFile.virtualFile ?: return@map null, editor) } ?: return@map null
    }.filterNotNull().joinToString("\n")
  }

  open fun getPathToElement(project: Project, virtualFile: VirtualFile, editor: Editor?): String? {
    return null
  }
}

class CopyAbsolutePathProvider : CopyPathProvider() {
  override fun getPathToElement(project: Project,
                                virtualFile: VirtualFile,
                                editor: Editor?): String? {
    return virtualFile.presentableUrl
  }
}

class CopyContentRootPathProvider : CopyPathProvider() {
  override fun getPathToElement(project: Project,
                                virtualFile: VirtualFile,
                                editor: Editor?): String? {
    return project.let {
      ProjectFileIndex.getInstance(project).getModuleForFile(virtualFile, false)?.let {
        ModuleRootManager.getInstance(it).contentRoots.mapNotNull { root -> VfsUtilCore.getRelativePath(virtualFile, root) }.singleOrNull()
      }
    }
  }
}

class CopyFileWithLineNumberPathProvider : CopyPathProvider() {
  override fun getPathToElement(project: Project,
                                virtualFile: VirtualFile,
                                editor: Editor?): String? =
    editor?.let { CopyReferenceUtil.getVirtualFileFqn(virtualFile, project) + ":" + (editor.caretModel.logicalPosition.line + 1) }
}

class CopySourceRootPathProvider : CopyPathProvider() {
  override fun getPathToElement(project: Project,
                                virtualFile: VirtualFile,
                                editor: Editor?): String? {
    return project.let {
      VfsUtilCore.getRelativePath(virtualFile, ProjectFileIndex.getInstance(project).getSourceRootForFile(virtualFile) ?: return null)
    }
  }
}

class CopyTBXReferenceProvider : CopyPathProvider() {
  override fun getQualifiedName(project: Project?,
                                elements: List<PsiElement>,
                                editor: Editor?): String =
    project?.let { CopyTBXReferenceAction.createJetbrainsLink(project, elements, editor) } ?: ""
}