// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("Declarations")

package com.intellij.model.psi.impl

import com.intellij.model.psi.PsiSymbolDeclaration
import com.intellij.model.psi.PsiSymbolDeclarationProvider
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementsAroundOffsetUp
import com.intellij.util.SmartList

private val declarationProviderEP = ExtensionPointName.create<PsiSymbolDeclarationProvider>("com.intellij.psi.declarationProvider")

private fun findDeclarationsInElement(element: PsiElement, offsetInElement: Int): Collection<PsiSymbolDeclaration> {
  val result = SmartList<PsiSymbolDeclaration>()
  for (extension: PsiSymbolDeclarationProvider in declarationProviderEP.extensions) {
    ProgressManager.checkCanceled()
    result += extension.getDeclarations(element, offsetInElement).filter {
      offsetInElement in it.declarationRange
    }
  }
  return result
}

/**
 * @return collection of declarations at the given [offset][offsetInFile] in [file]
 */
fun findDeclarationsAtOffset(file: PsiFile, offsetInFile: Int): Collection<PsiSymbolDeclaration> {
  val result = SmartList<PsiSymbolDeclaration>()
  for ((element: PsiElement, offsetInElement: Int) in file.elementsAroundOffsetUp(offsetInFile)) {
    ProgressManager.checkCanceled()
    result += findDeclarationsInElement(element, offsetInElement)
  }
  return result
}
