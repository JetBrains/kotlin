// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hint.actions

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.hint.*
import com.intellij.codeInsight.navigation.actions.TypeDeclarationProvider
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import com.intellij.util.Processor
import org.jetbrains.annotations.TestOnly
import java.awt.Component
import kotlin.streams.asSequence

open class ShowTypeDefinitionAction : ShowRelatedElementsActionBase() {
  override fun getSessionFactories(): List<ImplementationViewSessionFactory> = listOf(TypeDefinitionsViewSessionFactory)

  override fun getPopupTitle(session: ImplementationViewSession): String {
    return CodeInsightBundle.message("type.definition.view.title", session.text)
  }

  override fun couldPinPopup(): Boolean = false

  override fun triggerFeatureUsed(project: Project) {}

  override fun getIndexNotReadyMessage(): String {
    return CodeInsightBundle.message("show.type.definition.index.not.ready")
  }

  object TypeDefinitionsViewSessionFactory : ImplementationViewSessionFactory {
    override fun createSession(dataContext: DataContext,
                               project: Project,
                               isSearchDeep: Boolean,
                               alwaysIncludeSelf: Boolean): ImplementationViewSession? {
      val file = CommonDataKeys.PSI_FILE.getData(dataContext)
      val editor = PsiImplementationViewSession.getEditor(dataContext)
      val pair = PsiImplementationViewSession.getElementAndReference(dataContext, project, file, editor) ?: return null
      val element = pair.first
      return element?.let { TypeDefinitionViewSession(project, editor, file?.virtualFile, it) }
    }

    override fun createSessionForLookupElement(project: Project,
                                               editor: Editor?,
                                               file: VirtualFile?,
                                               lookupItemObject: Any?,
                                               isSearchDeep: Boolean,
                                               alwaysIncludeSelf: Boolean): ImplementationViewSession? {
      val psiFile = file?.let { PsiManager.getInstance(project).findFile(it) }
      val element = lookupItemObject as? PsiElement ?: DocumentationManager.getInstance(project).getElementFromLookup(editor, psiFile)
      if (element == null) return null
      val containingFile = element.containingFile
      if (containingFile == null || !containingFile.viewProvider.isPhysical) return null
      return TypeDefinitionViewSession(project, editor, file, element)
    }
  }

  class TypeDefinitionViewSession(override val project: Project,
                                  override val editor: Editor?,
                                  override val file: VirtualFile?,
                                  element: PsiElement) : ImplementationViewSession {
    override val text: String = SymbolPresentationUtil.getSymbolPresentableText(element)
    override val implementationElements: List<ImplementationViewElement> = searchTypeDefinitions(element)
    override val factory: ImplementationViewSessionFactory = TypeDefinitionsViewSessionFactory

    override fun elementRequiresIncludeSelf(): Boolean = false
    override fun needUpdateInBackground(): Boolean = false
    override fun dispose() {}

    override fun searchImplementationsInBackground(indicator: ProgressIndicator, processor: Processor<in ImplementationViewElement>)
      : List<ImplementationViewElement> = emptyList()

    companion object {
      private val PROGRESS_MESSAGE = CodeInsightBundle.message("searching.for.definitions")

      private fun searchTypeDefinitions(element: PsiElement): List<PsiImplementationViewElement> {
        val search = ThrowableComputable<List<PsiElement>, Exception> {
          TypeDeclarationProvider.EP_NAME.extensions().asSequence()
            .mapNotNull { provider ->
              ReadAction.compute<List<PsiElement>?, Throwable> {
                provider.getSymbolTypeDeclarations(element)?.mapNotNull { it?.navigationElement }
              }
            }
            .firstOrNull()
          ?: emptyList()
        }
        val definitions = ProgressManager.getInstance().runProcessWithProgressSynchronously(search, PROGRESS_MESSAGE, true, element.project)
        return definitions.map { PsiImplementationViewElement(it) }
      }
    }
  }

  companion object {
    @TestOnly
    fun runForTests(context: Component): List<PsiElement> {
      val showTypeDefinitionAction = ShowTypeDefinitionActionForTest()
      showTypeDefinitionAction.performForContext(DataManager.getInstance().getDataContext(context))
      return showTypeDefinitionAction.definitions.get().map { element -> (element as PsiImplementationViewElement).psiElement }
    }

    private class ShowTypeDefinitionActionForTest(val definitions: Ref<List<ImplementationViewElement>> = Ref()) : ShowTypeDefinitionAction() {
      override fun showImplementations(session: ImplementationViewSession, invokedFromEditor: Boolean, invokedByShortcut: Boolean) =
        definitions.set(session.implementationElements)
    }
  }
}