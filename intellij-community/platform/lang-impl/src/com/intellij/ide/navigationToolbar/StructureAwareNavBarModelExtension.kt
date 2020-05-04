// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.navigationToolbar

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.ide.ui.UISettings
import com.intellij.ide.util.treeView.smartTree.NodeProvider
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.Language
import com.intellij.lang.LanguageStructureViewBuilder
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.reference.SoftReference
import com.intellij.util.Processor

/**
 * @author yole
 */
abstract class StructureAwareNavBarModelExtension : AbstractNavBarModelExtension() {
  protected abstract val language: Language
  private var currentFile: SoftReference<PsiFile>? = null
  private var currentFileStructure: SoftReference<StructureViewModel>? = null
  private var currentFileModCount = -1L

  override fun getLeafElement(dataContext: DataContext): PsiElement? {
    if (UISettings.instance.showMembersInNavigationBar) {
      val psiFile = CommonDataKeys.PSI_FILE.getData(dataContext)
      val editor = CommonDataKeys.EDITOR.getData(dataContext)
      if (psiFile == null || editor == null) return null
      val psiElement = psiFile.findElementAt(editor.caretModel.offset)
      if (psiElement?.language === language) {
        buildStructureViewModel(psiFile, editor)?.let { model ->
          return (model.currentEditorElement as? PsiElement)?.originalElement
        }
      }
    }
    return null
  }

  override fun processChildren(`object`: Any,
                               rootElement: Any?,
                               processor: Processor<Any>): Boolean {
    if (UISettings.instance.showMembersInNavigationBar) {
      (`object` as? PsiElement)?.let { psiElement ->
        if (psiElement.language == language) {
          buildStructureViewModel(psiElement.containingFile)?.let { model ->
            return processStructureViewChildren(model.root, `object`, processor)
          }
        }
      }
    }
    return super.processChildren(`object`, rootElement, processor)
  }

  override fun getParent(psiElement: PsiElement?): PsiElement? {
    if (psiElement?.language == language) {
      val file = psiElement.containingFile ?: return null
      val model = buildStructureViewModel(file)
      if (model != null) {
        val parentInModel = findParentInModel(model.root, psiElement)
        if (acceptParentFromModel(parentInModel)) {
          return parentInModel
        }
      }
    }
    return super.getParent(psiElement)
  }

  protected open fun acceptParentFromModel(psiElement: PsiElement?): Boolean {
    return true
  }

  private fun findParentInModel(root: StructureViewTreeElement, psiElement: PsiElement): PsiElement? {
    for (child in childrenFromNodeAndProviders(root)) {
      if ((child as StructureViewTreeElement).value == psiElement) {
        return root.value as? PsiElement
      }
      findParentInModel(child, psiElement)?.let { return it }
    }
    return null
  }

  private fun buildStructureViewModel(file: PsiFile, editor: Editor? = null): StructureViewModel? {
    if (currentFile?.get() == file && currentFileModCount == file.modificationStamp) {
      currentFileStructure?.get()?.let { return it }
    }

    val builder = LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(file)
    val model = (builder as? TreeBasedStructureViewBuilder)?.createStructureViewModel(editor)
    if (model != null) {
      currentFile = SoftReference(file)
      currentFileStructure = SoftReference(model)
      currentFileModCount = file.modificationStamp
    }
    return model
  }

  private fun processStructureViewChildren(parent: StructureViewTreeElement,
                                           `object`: Any,
                                           processor: Processor<Any>): Boolean {
    if (parent.value == `object`) {
      return childrenFromNodeAndProviders(parent)
        .filterIsInstance<StructureViewTreeElement>()
        .all { processor.process(it.value) }
    }

    return childrenFromNodeAndProviders(parent)
      .filterIsInstance<StructureViewTreeElement>()
      .all { processStructureViewChildren(it, `object`, processor) }
  }

  private fun childrenFromNodeAndProviders(parent: StructureViewTreeElement): List<TreeElement> {
    val children = if (parent is PsiTreeElementBase<*>) parent.childrenWithoutCustomRegions else parent.children.toList()
    return children + applicableNodeProviders.flatMap { it.provideNodes(parent) }
  }

  override fun normalizeChildren() = false

  protected open val applicableNodeProviders: List<NodeProvider<*>> = emptyList()
}