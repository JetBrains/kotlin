// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hint

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.hint.PsiImplementationViewSession.getSelfAndImplementations
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import com.intellij.usageView.UsageInfo
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.util.Processor

interface ImplementationViewSession : Disposable {
  val factory: ImplementationViewSessionFactory
  val project: Project

  /**
   * The list of implementations which could be found synchronously. Additional implementations can be obtained by calling
   * [searchImplementationsInBackground].
   */
  val implementationElements: List<ImplementationViewElement>
  val file: VirtualFile?

  val text: String?
  val editor: Editor?

  fun searchImplementationsInBackground(indicator: ProgressIndicator,
                                        processor: Processor<in ImplementationViewElement>): List<ImplementationViewElement>
  fun elementRequiresIncludeSelf(): Boolean
  fun needUpdateInBackground(): Boolean
}

interface ImplementationViewSessionFactory {
  fun createSession(
    dataContext: DataContext,
    project: Project,
    isSearchDeep: Boolean,
    alwaysIncludeSelf: Boolean
  ): ImplementationViewSession?

  fun createSessionForLookupElement(
    project: Project,
    editor: Editor?,
    file: VirtualFile?,
    lookupItemObject: Any?,
    isSearchDeep: Boolean,
    alwaysIncludeSelf: Boolean
  ): ImplementationViewSession?

  companion object {
    @JvmField val EP_NAME = ExtensionPointName.create<ImplementationViewSessionFactory>("com.intellij.implementationViewSessionFactory")
  }
}

class PsiImplementationSessionViewFactory : ImplementationViewSessionFactory {
  override fun createSession(
    dataContext: DataContext,
    project: Project,
    isSearchDeep: Boolean,
    alwaysIncludeSelf: Boolean
  ): ImplementationViewSession? {
    return PsiImplementationViewSession.create(dataContext, project, isSearchDeep, alwaysIncludeSelf)
  }

  override fun createSessionForLookupElement(project: Project,
                                             editor: Editor?,
                                             file: VirtualFile?,
                                             lookupItemObject: Any?,
                                             isSearchDeep: Boolean,
                                             alwaysIncludeSelf: Boolean): ImplementationViewSession? {
    val psiFile = file?.let { PsiManager.getInstance(project).findFile(it) }
    val element = lookupItemObject as? PsiElement ?: DocumentationManager.getInstance(project).getElementFromLookup(editor, psiFile)
    var impls = arrayOf<PsiElement>()
    var text = ""
    if (element != null) {
      // if (element instanceof PsiPackage) return;
      val containingFile = element.containingFile
      if (containingFile == null || !containingFile.viewProvider.isPhysical) return null

      impls = getSelfAndImplementations(editor, element, PsiImplementationViewSession.createImplementationsSearcher(isSearchDeep))
      text = SymbolPresentationUtil.getSymbolPresentableText(element)
    }

    return PsiImplementationViewSession(project, element, impls, text, editor, file, isSearchDeep, alwaysIncludeSelf)
  }
}
