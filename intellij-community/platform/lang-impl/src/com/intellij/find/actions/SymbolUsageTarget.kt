// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("DEPRECATION")

package com.intellij.find.actions

import com.intellij.find.usages.UsageHandler
import com.intellij.find.usages.impl.AllSearchOptions
import com.intellij.find.usages.impl.createUsageHandler
import com.intellij.model.Pointer
import com.intellij.model.Symbol
import com.intellij.model.presentation.SymbolPresentationService
import com.intellij.model.presentation.SymbolPresentationService.getLongDescription
import com.intellij.model.psi.PsiSymbolService
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.TypeSafeDataProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usages.ConfigurableUsageTarget
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageView
import com.intellij.usages.impl.UsageViewImpl
import javax.swing.Icon

internal class SymbolUsageTarget<O>(
  private val project: Project,
  symbol: Symbol,
  private val allOptions: AllSearchOptions<O>
) : UsageTarget, TypeSafeDataProvider, ConfigurableUsageTarget {

  private val myPointer: Pointer<out Symbol> = symbol.createPointer()
  override fun isValid(): Boolean = myPointer.dereference() != null

  // ----- presentation -----

  private var myItemPresentation: ItemPresentation = getItemPresentation(symbol)

  override fun update() {
    val symbol = myPointer.dereference() ?: return
    myItemPresentation = getItemPresentation(symbol)
  }

  private fun getItemPresentation(symbol: Symbol): ItemPresentation {
    val symbolPresentation = SymbolPresentationService.getInstance().getSymbolPresentation(symbol)
    return object : ItemPresentation {
      override fun getIcon(unused: Boolean): Icon? = symbolPresentation.icon
      override fun getPresentableText(): String? = symbolPresentation.longDescription
      override fun getLocationString(): String? = error("must not be called")
    }
  }

  override fun getPresentation(): ItemPresentation = myItemPresentation

  override fun isReadOnly(): Boolean = false // TODO used in Usage View displayed by refactorings

  // ----- Navigatable & NavigationItem -----
  // TODO Symbol navigation

  private val psi: PsiElement?
    get() {
      val symbol = myPointer.dereference() ?: return null
      return PsiSymbolService.getInstance().extractElementFromSymbol(symbol)
    }

  override fun getName(): String? = (psi as? NavigationItem)?.name

  override fun navigate(requestFocus: Boolean) {
    (psi as? Navigatable)?.let { navigatable ->
      if (navigatable.canNavigate()) {
        navigatable.navigate(requestFocus)
      }
    }
  }

  override fun canNavigate(): Boolean = (psi as? Navigatable)?.canNavigate() == true

  override fun canNavigateToSource(): Boolean = (psi as? Navigatable)?.canNavigateToSource() == true

  // ----- actions -----

  override fun getShortcut(): KeyboardShortcut? = UsageViewImpl.getShowUsagesWithSettingsShortcut()

  override fun getLongDescriptiveName(): String {
    val symbol = myPointer.dereference() ?: return UsageViewBundle.message("node.invalid")
    @Suppress("UNCHECKED_CAST") val usageHandler = symbol.createUsageHandler(project) as UsageHandler<O>
    return UsageViewBundle.message(
      "search.title.0.in.1",
      usageHandler.getSearchString(allOptions.options, allOptions.customOptions),
      allOptions.options.searchScope.displayName
    )
  }

  override fun showSettings() {
    val symbol = myPointer.dereference() ?: return
    @Suppress("UNCHECKED_CAST") val usageHandler = symbol.createUsageHandler(project) as UsageHandler<O>
    val dialog = UsageOptionsDialog(project, getLongDescription(symbol), usageHandler, allOptions, true)
    if (!dialog.showAndGet()) {
      return
    }
    val newOptions = dialog.result()
    findUsages(project, symbol, usageHandler, newOptions)
  }

  override fun findUsages(): Unit = error("must not be called")
  override fun findUsagesInEditor(editor: FileEditor): Unit = error("must not be called")
  override fun highlightUsages(file: PsiFile, editor: Editor, clearHighlights: Boolean): Unit = error("must not be called")

  // ----- data context -----

  override fun getFiles(): Array<VirtualFile?>? = psi?.containingFile?.virtualFile?.let { arrayOf(it) }

  override fun calcData(key: DataKey<*>, sink: DataSink) {
    if (key === UsageView.USAGE_INFO_KEY) {
      val element = psi
      if (element != null && element.textRange != null) {
        sink.put(UsageView.USAGE_INFO_KEY, UsageInfo(element))
      }
    }
    else if (key === UsageView.USAGE_SCOPE) {
      sink.put(UsageView.USAGE_SCOPE, allOptions.options.searchScope)
    }
  }
}
