// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.psi.impl

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolDeclaration
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.model.psi.PsiSymbolService
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.ImaginaryEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.util.SmartList
import org.jetbrains.annotations.ApiStatus.Experimental

/**
 * Entry point for obtaining target symbols by [offset] in a [file].
 *
 * @return collection of referenced or declared symbols
 */
@Experimental
fun targetSymbols(file: PsiFile, offset: Int): Collection<Symbol> {
  val referencedSymbols: Collection<Symbol> = referencedSymbols(file, offset)
  if (referencedSymbols.isNotEmpty()) {
    return referencedSymbols
  }
  return declaredSymbols(file, offset)
}

private fun referencedSymbols(file: PsiFile, offset: Int): Collection<Symbol> {
  val result = SmartList<Symbol>()
  for (reference: PsiSymbolReference in file.allReferencesAround(offset)) {
    reference.resolveReference().mapTo(result) {
      it.target
    }
  }
  result += fromTargetEvaluator(file, offset)
  return result
}

private fun fromTargetEvaluator(file: PsiFile, offset: Int): Collection<Symbol> {
  val project = file.project
  val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return emptyList()
  val editor = mockEditor(project, document)
  val flags = TargetElementUtil.getInstance().allAccepted and
    TargetElementUtil.ELEMENT_NAME_ACCEPTED.inv() and
    TargetElementUtil.LOOKUP_ITEM_ACCEPTED.inv()
  val targetElement = TargetElementUtil.getInstance().findTargetElement(editor, flags, offset)
  if (targetElement != null) {
    return listOf(PsiSymbolService.getInstance().asSymbol(targetElement))
  }
  val reference = TargetElementUtil.findReference(editor, offset) ?: return emptyList()
  return TargetElementUtil.getInstance().getTargetCandidates(reference).map(PsiSymbolService.getInstance()::asSymbol)
}

private fun mockEditor(project: Project, document: Document): Editor {
  return object : ImaginaryEditor(project, document) {
    override fun toString(): String = "API compatibility editor"
  }
}

private fun declaredSymbols(file: PsiFile, offset: Int): Collection<Symbol> {
  val result = SmartList<Symbol>()
  file.allDeclarationsAround(offset).mapTo(result, PsiSymbolDeclaration::getSymbol)
  return result
}
