// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hint

import com.intellij.navigation.NavigationItem
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiBinaryFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import javax.swing.Icon

/**
 * A single element shown in the Show Implementations view.
 *
 * @author yole
 */
abstract class ImplementationViewElement {
  abstract val project: Project
  abstract val isNamed: Boolean
  abstract val name: String?
  abstract val presentableText: String
  abstract val containingFile: VirtualFile?
  abstract val text: String?
  abstract val locationText: String?
  abstract val locationIcon: Icon?
  abstract val containingMemberOrSelf: ImplementationViewElement
  abstract val elementForShowUsages: PsiElement?

  abstract fun navigate(focusEditor: Boolean)

  open val usage: Usage?
    get() {
      return UsageInfo2UsageAdapter(UsageInfo(elementForShowUsages ?: return null))
    }
}

class PsiImplementationViewElement(val psiElement: PsiElement) : ImplementationViewElement() {
  override val project: Project
    get() = psiElement.project

  override val isNamed: Boolean
    get() = psiElement is PsiNamedElement

  override val name: String?
    get() = (psiElement as? PsiNamedElement)?.name

  override val containingFile: VirtualFile?
    get() = psiElement.containingFile?.originalFile?.virtualFile

  override val text: String?
    get() = ImplementationViewComponent.getNewText(psiElement)

  override val presentableText: String
    get() {
      val presentation = (psiElement as? NavigationItem)?.presentation
      val vFile = containingFile ?: return ""
      val presentableName = vFile.presentableName
      if (presentation == null) {
        return presentableName
      }

      val elementPresentation  = presentation.presentableText
      val locationString = presentation.locationString

      return if (vFile.name == elementPresentation + "." + vFile.extension) {
        presentableName + if (!StringUtil.isEmptyOrSpaces(locationString)) " $locationString" else ""
      }
      else {
        "$presentableName ($elementPresentation)"
      }
    }

  override val locationText: String?
    get() = ElementLocationUtil.renderElementLocation(psiElement, Ref())

  override val locationIcon: Icon?
    get() = Ref<Icon>().also { ElementLocationUtil.renderElementLocation(psiElement, it) }.get()

  override val containingMemberOrSelf: ImplementationViewElement
    get() {
      val parent = PsiTreeUtil.getStubOrPsiParent(psiElement)
      if (parent == null || (parent is PsiFile && parent.virtualFile == containingFile)) {
        return this
      }
      return PsiImplementationViewElement(parent)
    }

  override fun navigate(focusEditor: Boolean) {
    val navigationElement = psiElement.navigationElement
    val file = navigationElement.containingFile?.originalFile ?: return
    val virtualFile = file.virtualFile ?: return
    val project = psiElement.project
    val fileEditorManager = FileEditorManagerEx.getInstanceEx(project)
    val descriptor = OpenFileDescriptor(project, virtualFile, navigationElement.textOffset)
    fileEditorManager.openTextEditor(descriptor, focusEditor)
  }

  override val elementForShowUsages: PsiElement?
    get() = if (psiElement !is PsiBinaryFile) psiElement else null
}
